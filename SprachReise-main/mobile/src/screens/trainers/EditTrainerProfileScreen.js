import { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  Image,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as ImagePicker from 'expo-image-picker';
import { COLORS, FONTS } from '../../constants/config';
import { trainersAPI, fileUrl } from '../../services/api';
import { useAuthStore } from '../../store/authStore';

const BIO_MAX = 1000;

export default function EditTrainerProfileScreen({ navigation }) {
  const { user, setAuth, accessToken } = useAuthStore();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [photoUrl, setPhotoUrl] = useState(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [city, setCity] = useState('');
  const [bio, setBio] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const res = await trainersAPI.getMe();
        const p = res.data.profile;
        setFirstName(p.firstName || '');
        setLastName(p.lastName || '');
        setPhone(user?.phone || '');
        setCity(p.city || '');
        setBio(p.bio || '');
        setPhotoUrl(p.photoUrl || null);
      } catch (e) {
        Alert.alert('Erreur', e.response?.data?.error || e.message || 'Chargement impossible');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const pickPhoto = async () => {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) {
      Alert.alert('Permission refusée', 'Active l\'accès à la galerie dans les réglages.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.7,
    });
    if (result.canceled || !result.assets?.[0]) return;

    const asset = result.assets[0];
    const uri = asset.uri;
    const name = asset.fileName || uri.split('/').pop() || `photo_${Date.now()}.jpg`;
    const ext = name.split('.').pop().toLowerCase();
    const type = ext === 'png' ? 'image/png' : ext === 'webp' ? 'image/webp' : 'image/jpeg';

    setUploadingPhoto(true);
    try {
      const res = await trainersAPI.uploadPhoto(uri, name, type);
      setPhotoUrl(res.data.photoUrl);
      if (user) await setAuth({ ...user, photoUrl: res.data.photoUrl }, accessToken);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Échec de l\'upload');
    } finally {
      setUploadingPhoto(false);
    }
  };

  const save = async () => {
    if (!firstName.trim() || !lastName.trim()) {
      Alert.alert('Champs requis', 'Prénom et nom sont obligatoires.');
      return;
    }
    if (bio.length > BIO_MAX) {
      Alert.alert('Biographie trop longue', `Maximum ${BIO_MAX} caractères.`);
      return;
    }
    setSaving(true);
    try {
      const res = await trainersAPI.updateMe({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phone: phone.trim(),
        city: city.trim(),
        bio: bio.trim(),
      });
      const p = res.data.profile;
      if (user) {
        await setAuth({
          ...user,
          firstName: p.firstName,
          lastName: p.lastName,
          photoUrl: p.photoUrl,
        }, accessToken);
      }
      Alert.alert('Profil enregistré', 'Tes modifications sont visibles.', [
        { text: 'OK', onPress: () => navigation.goBack() },
      ]);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Enregistrement impossible');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color={COLORS.gold} />
      </SafeAreaView>
    );
  }

  const initials = `${firstName?.[0] || ''}${lastName?.[0] || ''}`.toUpperCase();
  const photoFull = fileUrl(photoUrl);

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <TouchableOpacity style={styles.backLink} onPress={() => navigation.goBack()}>
            <Text style={styles.backLinkText}>‹ Retour</Text>
          </TouchableOpacity>

          <Text style={styles.heading}>Modifier mon profil</Text>

          <View style={styles.photoBlock}>
            <TouchableOpacity onPress={pickPhoto} activeOpacity={0.85} style={styles.avatarWrap}>
              {photoFull ? (
                <Image source={{ uri: photoFull }} style={styles.avatarImg} />
              ) : (
                <View style={styles.avatarPlaceholder}>
                  <Text style={styles.avatarText}>{initials || '👤'}</Text>
                </View>
              )}
              {uploadingPhoto && (
                <View style={styles.uploadOverlay}>
                  <ActivityIndicator size="small" color={COLORS.parchment} />
                </View>
              )}
            </TouchableOpacity>
            <TouchableOpacity onPress={pickPhoto} disabled={uploadingPhoto}>
              <Text style={styles.changePhoto}>
                {uploadingPhoto ? 'Envoi…' : (photoFull ? 'Changer la photo' : 'Ajouter une photo')}
              </Text>
            </TouchableOpacity>
          </View>

          <Field label="Prénom" value={firstName} onChangeText={setFirstName} />
          <Field label="Nom" value={lastName} onChangeText={setLastName} />
          <Field label="Téléphone" value={phone} onChangeText={setPhone} keyboardType="phone-pad" />
          <Field label="Ville" value={city} onChangeText={setCity} />

          <View style={styles.field}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>Biographie</Text>
              <Text style={styles.counter}>{bio.length}/{BIO_MAX}</Text>
            </View>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={bio}
              onChangeText={(t) => setBio(t.slice(0, BIO_MAX))}
              multiline
              numberOfLines={6}
              placeholder="Parle de ton parcours, tes spécialités…"
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
          </View>

          <TouchableOpacity
            style={[styles.saveBtn, saving && styles.saveBtnDisabled]}
            onPress={save}
            disabled={saving}
          >
            {saving ? (
              <ActivityIndicator color={COLORS.parchment} />
            ) : (
              <Text style={styles.saveBtnText}>ENREGISTRER</Text>
            )}
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Field({ label, ...inputProps }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        style={styles.input}
        placeholderTextColor="rgba(174,145,130,0.5)"
        {...inputProps}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },
  scroll: { padding: 20, paddingBottom: 40 },
  center: { alignItems: 'center', justifyContent: 'center' },

  backLink: { marginBottom: 8 },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },

  heading: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 24,
    marginBottom: 24,
  },

  photoBlock: { alignItems: 'center', marginBottom: 28 },
  avatarWrap: { width: 110, height: 110, marginBottom: 10 },
  avatarImg: {
    width: 110,
    height: 110,
    borderRadius: 55,
    borderWidth: 2,
    borderColor: COLORS.accent,
  },
  avatarPlaceholder: {
    width: 110,
    height: 110,
    borderRadius: 55,
    backgroundColor: 'rgba(161,94,45,0.2)',
    borderWidth: 2,
    borderColor: COLORS.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: { fontFamily: FONTS.displayBold, color: COLORS.gold, fontSize: 36 },
  uploadOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.45)',
    borderRadius: 55,
    alignItems: 'center',
    justifyContent: 'center',
  },
  changePhoto: {
    fontFamily: FONTS.uiBold,
    color: COLORS.gold,
    fontSize: 12,
    letterSpacing: 1,
    marginTop: 4,
  },

  field: { marginBottom: 16 },
  labelRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  label: {
    fontFamily: FONTS.uiBold,
    color: COLORS.muted,
    fontSize: 11,
    letterSpacing: 1.2,
    marginBottom: 6,
  },
  counter: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 11 },
  input: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontFamily: FONTS.regular,
    color: COLORS.parchment,
    fontSize: 14,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
  },
  textarea: { minHeight: 110, textAlignVertical: 'top' },

  saveBtn: {
    backgroundColor: COLORS.accent,
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 12,
  },
  saveBtnDisabled: { opacity: 0.6 },
  saveBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 13,
    letterSpacing: 1.2,
  },
});
