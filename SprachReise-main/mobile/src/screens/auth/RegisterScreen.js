import { useState } from 'react';
import {
  View, Text, StyleSheet, TextInput, TouchableOpacity,
  ActivityIndicator, KeyboardAvoidingView, Platform, ScrollView, Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as DocumentPicker from 'expo-document-picker';
import { COLORS, FONTS, LEVELS } from '../../constants/config';
import { authAPI, trainerApplicationsAPI } from '../../services/api';

const BIO_MAX = 1000;
const MAX_PDF_BYTES = 5 * 1024 * 1024;

export default function RegisterScreen({ navigation }) {
  const [role, setRole] = useState('LEARNER'); // 'LEARNER' | 'TRAINER'

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', phone: '', password: '', confirm: '',
  });

  // Champs spécifiques formateur (candidature)
  const [requestedLevelCode, setRequestedLevelCode] = useState('B1');
  const [nativeLanguage, setNativeLanguage] = useState('');
  const [motivation, setMotivation] = useState('');
  const [bio, setBio] = useState('');
  const [diploma, setDiploma] = useState(null);

  const [loading, setLoading] = useState(false);
  const [submittedApp, setSubmittedApp] = useState(null);

  const update = (key, val) => setForm((f) => ({ ...f, [key]: val }));

  const pickDiploma = async () => {
    try {
      const res = await DocumentPicker.getDocumentAsync({
        type: 'application/pdf',
        copyToCacheDirectory: true,
        multiple: false,
      });
      if (res.canceled || !res.assets?.[0]) return;
      const asset = res.assets[0];
      if (asset.size && asset.size > MAX_PDF_BYTES) {
        Alert.alert('Fichier trop volumineux', 'Le diplôme doit faire 5 Mo maximum.');
        return;
      }
      setDiploma({
        uri: asset.uri,
        name: asset.name || `diploma_${Date.now()}.pdf`,
        type: asset.mimeType || 'application/pdf',
        size: asset.size,
      });
    } catch (e) {
      Alert.alert('Erreur', e.message || 'Sélection impossible');
    }
  };

  const handleLearnerRegister = async () => {
    if (!form.firstName || !form.lastName || !form.email || !form.password) {
      Alert.alert('Erreur', 'Veuillez remplir tous les champs obligatoires');
      return;
    }
    if (form.password !== form.confirm) {
      Alert.alert('Erreur', 'Les mots de passe ne correspondent pas');
      return;
    }
    if (form.password.length < 8) {
      Alert.alert('Erreur', 'Le mot de passe doit contenir au moins 8 caractères');
      return;
    }
    setLoading(true);
    try {
      const { data } = await authAPI.register({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email.trim().toLowerCase(),
        password: form.password,
        phone: form.phone,
      });
      navigation.navigate('Pricing', {
        authData: {
          user: {
            id: data.id,
            email: data.email,
            firstName: data.firstName,
            lastName: data.lastName,
            role: data.role,
            photoUrl: data.photoUrl,
          },
          accessToken: data.accessToken,
        },
      });
    } catch (err) {
      const msg = err.response?.data?.error || 'Erreur lors de l\'inscription';
      Alert.alert('Inscription impossible', msg);
    } finally {
      setLoading(false);
    }
  };

  const handleTrainerApply = async () => {
    if (!form.firstName.trim() || !form.lastName.trim() || !form.email.trim()) {
      Alert.alert('Champs requis', 'Prénom, nom et email sont obligatoires.');
      return;
    }
    if (!nativeLanguage.trim()) {
      Alert.alert('Champ requis', 'Indique ta langue maternelle.');
      return;
    }
    if (!bio.trim()) {
      Alert.alert('Champ requis', 'La biographie est obligatoire.');
      return;
    }
    if (bio.length > BIO_MAX) {
      Alert.alert('Bio trop longue', `Maximum ${BIO_MAX} caractères.`);
      return;
    }
    if (!motivation.trim()) {
      Alert.alert('Champ requis', 'Explique ta motivation.');
      return;
    }
    if (!diploma) {
      Alert.alert('Diplôme requis', 'Tu dois joindre au moins un diplôme PDF (max 5 Mo).');
      return;
    }
    setLoading(true);
    try {
      const { data } = await trainerApplicationsAPI.submit(
        {
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          email: form.email.trim().toLowerCase(),
          phone: form.phone.trim(),
          bio: bio.trim(),
          requestedLevelCode,
          nativeLanguage: nativeLanguage.trim(),
          motivation: motivation.trim(),
        },
        diploma,
      );
      setSubmittedApp({ id: data.id || data.applicationId });
    } catch (err) {
      const msg = err.response?.data?.error || err.message || 'Envoi impossible';
      Alert.alert('Candidature impossible', msg);
    } finally {
      setLoading(false);
    }
  };

  if (submittedApp) {
    return (
      <SafeAreaView style={styles.container}>
        <ScrollView contentContainerStyle={styles.confirmScroll}>
          <Text style={styles.confirmIcon}>✓</Text>
          <Text style={styles.confirmTitle}>Candidature envoyée</Text>
          <Text style={styles.confirmText}>
            Ta candidature de formateur a bien été enregistrée
            {submittedApp.id ? ` (n° ${submittedApp.id})` : ''}.
            {'\n\n'}L'administration va examiner ton dossier dans les 48 prochaines heures.
            Si elle est acceptée, tu recevras un email avec tes identifiants formateur.
          </Text>
          <TouchableOpacity style={styles.button} onPress={() => navigation.navigate('Login')}>
            <Text style={styles.buttonText}>RETOUR À LA CONNEXION</Text>
          </TouchableOpacity>
        </ScrollView>
      </SafeAreaView>
    );
  }

  const isTrainer = role === 'TRAINER';

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">

          <TouchableOpacity onPress={() => navigation.goBack()} style={styles.back}>
            <Text style={styles.backText}>‹ Retour</Text>
          </TouchableOpacity>

          <Text style={styles.heading}>Créer un compte</Text>
          <Text style={styles.subheading}>Rejoignez SprachReise</Text>

          <Text style={styles.label}>Je suis *</Text>
          <View style={styles.roleRow}>
            <TouchableOpacity
              style={[styles.roleChip, role === 'LEARNER' && styles.roleChipActive]}
              onPress={() => setRole('LEARNER')}
            >
              <Text style={styles.roleEmoji}>📚</Text>
              <Text style={[styles.roleLabel, role === 'LEARNER' && styles.roleLabelActive]}>
                Apprenant
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.roleChip, role === 'TRAINER' && styles.roleChipActive]}
              onPress={() => setRole('TRAINER')}
            >
              <Text style={styles.roleEmoji}>🎓</Text>
              <Text style={[styles.roleLabel, role === 'TRAINER' && styles.roleLabelActive]}>
                Formateur
              </Text>
            </TouchableOpacity>
          </View>

          {isTrainer && (
            <View style={styles.trainerHint}>
              <Text style={styles.trainerHintText}>
                Ta candidature sera examinée par l'administration. Tu recevras tes identifiants formateur par email après validation.
              </Text>
            </View>
          )}

          <Field label="Prénom *" value={form.firstName} onChangeText={(v) => update('firstName', v)} placeholder="Jean" />
          <Field label="Nom *" value={form.lastName} onChangeText={(v) => update('lastName', v)} placeholder="Dupont" />
          <Field label="Email *" value={form.email} onChangeText={(v) => update('email', v)} placeholder="jean@exemple.com" keyboardType="email-address" autoCapitalize="none" />
          <Field label="Téléphone" value={form.phone} onChangeText={(v) => update('phone', v)} placeholder="+237 6XX XXX XXX" keyboardType="phone-pad" />

          {/* Champs LEARNER : mot de passe */}
          {!isTrainer && (
            <>
              <Field label="Mot de passe *" value={form.password} onChangeText={(v) => update('password', v)} placeholder="Minimum 8 caractères" secureTextEntry />
              <Field label="Confirmer le mot de passe *" value={form.confirm} onChangeText={(v) => update('confirm', v)} placeholder="••••••••" secureTextEntry />
            </>
          )}

          {/* Champs FORMATEUR : candidature */}
          {isTrainer && (
            <>
              <Text style={styles.label}>Niveau d'enseignement souhaité *</Text>
              <View style={styles.levelsRow}>
                {LEVELS.map((lvl) => (
                  <TouchableOpacity
                    key={lvl}
                    style={[styles.levelChip, requestedLevelCode === lvl && styles.levelChipActive]}
                    onPress={() => setRequestedLevelCode(lvl)}
                  >
                    <Text style={[styles.levelText, requestedLevelCode === lvl && styles.levelTextActive]}>
                      {lvl}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Field
                label="Langue maternelle *"
                value={nativeLanguage}
                onChangeText={setNativeLanguage}
                placeholder="Français, Allemand, ..."
              />

              <View style={styles.labelRow}>
                <Text style={styles.label}>Biographie *</Text>
                <Text style={styles.counter}>{bio.length}/{BIO_MAX}</Text>
              </View>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={bio}
                onChangeText={(t) => setBio(t.slice(0, BIO_MAX))}
                placeholder="Parle de ton parcours, tes diplômes, ta spécialité..."
                placeholderTextColor={COLORS.muted}
                multiline
                numberOfLines={4}
              />

              <Text style={styles.label}>Motivation *</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={motivation}
                onChangeText={setMotivation}
                placeholder="Pourquoi veux-tu enseigner sur SprachReise ?"
                placeholderTextColor={COLORS.muted}
                multiline
                numberOfLines={3}
              />

              <Text style={styles.label}>Diplôme de langue (PDF, 5 Mo max) *</Text>
              <TouchableOpacity style={styles.fileBtn} onPress={pickDiploma}>
                <Text style={styles.fileBtnText}>
                  {diploma ? `📎 ${diploma.name}` : '+ Choisir un fichier PDF'}
                </Text>
              </TouchableOpacity>
              <Text style={styles.fileHint}>
                Accepté : Goethe-Zertifikat, telc Deutsch, TestDaF, DSH, ZD, Licence/Master en langues
              </Text>
            </>
          )}

          <TouchableOpacity
            style={[styles.button, loading && { opacity: 0.6 }]}
            onPress={isTrainer ? handleTrainerApply : handleLearnerRegister}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color={COLORS.parchment} />
            ) : (
              <Text style={styles.buttonText}>
                {isTrainer ? 'ENVOYER MA CANDIDATURE' : 'CRÉER MON COMPTE'}
              </Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity onPress={() => navigation.navigate('Login')} style={styles.loginLink}>
            <Text style={styles.loginText}>
              Déjà un compte ?{'  '}
              <Text style={styles.loginTextHighlight}>Se connecter</Text>
            </Text>
          </TouchableOpacity>

        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Field({ label, ...props }) {
  return (
    <>
      <Text style={styles.label}>{label}</Text>
      <TextInput style={styles.input} placeholderTextColor={COLORS.muted} {...props} />
    </>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },
  scroll: { flexGrow: 1, paddingHorizontal: 28, paddingBottom: 40 },

  back: { paddingTop: 16, paddingBottom: 4 },
  backText: { fontFamily: FONTS.regular, color: COLORS.gold, fontSize: 16 },

  heading: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 30,
    marginTop: 12,
    marginBottom: 6,
  },
  subheading: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 15,
    marginBottom: 8,
  },

  label: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.cream,
    fontSize: 13,
    marginBottom: 7,
    marginTop: 16,
    letterSpacing: 0.3,
  },
  labelRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  counter: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 11, marginTop: 16 },
  input: {
    backgroundColor: 'rgba(249,244,232,0.07)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.4)',
    borderRadius: 6,
    padding: 14,
    color: COLORS.parchment,
    fontSize: 15,
    fontFamily: FONTS.regular,
  },
  textarea: { minHeight: 90, textAlignVertical: 'top' },

  roleRow: { flexDirection: 'row', gap: 12 },
  roleChip: {
    flex: 1,
    backgroundColor: 'rgba(249,244,232,0.05)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.4)',
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
  },
  roleChipActive: {
    backgroundColor: 'rgba(184,137,58,0.15)',
    borderColor: COLORS.gold,
  },
  roleEmoji: { fontSize: 22, marginBottom: 4 },
  roleLabel: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.muted,
    fontSize: 13,
    letterSpacing: 0.3,
  },
  roleLabelActive: { color: COLORS.gold, fontFamily: FONTS.uiBold },

  trainerHint: {
    backgroundColor: 'rgba(184,137,58,0.1)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.3)',
    padding: 12,
    marginTop: 16,
  },
  trainerHintText: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 13,
    lineHeight: 19,
  },

  levelsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  levelChip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 6,
    backgroundColor: 'rgba(249,244,232,0.06)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.3)',
  },
  levelChipActive: { backgroundColor: COLORS.accent, borderColor: COLORS.accent },
  levelText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 13 },
  levelTextActive: { color: COLORS.parchment, fontFamily: FONTS.uiBold },

  fileBtn: {
    backgroundColor: 'rgba(249,244,232,0.07)',
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.4)',
    borderRadius: 6,
    borderStyle: 'dashed',
    padding: 14,
    alignItems: 'center',
  },
  fileBtnText: { fontFamily: FONTS.uiMedium, color: COLORS.gold, fontSize: 13 },
  fileHint: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 11,
    marginTop: 6,
    fontStyle: 'italic',
  },

  button: {
    backgroundColor: COLORS.accent,
    padding: 16,
    borderRadius: 6,
    alignItems: 'center',
    marginTop: 28,
  },
  buttonText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 14,
    letterSpacing: 1.5,
  },

  loginLink: { alignItems: 'center', marginTop: 20 },
  loginText: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 14 },
  loginTextHighlight: { fontFamily: FONTS.medium, color: COLORS.gold },

  confirmScroll: { padding: 32, alignItems: 'center', flexGrow: 1, justifyContent: 'center' },
  confirmIcon: {
    fontSize: 56,
    color: COLORS.success,
    marginBottom: 16,
    fontFamily: FONTS.displayBold,
  },
  confirmTitle: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 26,
    marginBottom: 12,
    textAlign: 'center',
  },
  confirmText: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 14,
    lineHeight: 21,
    textAlign: 'center',
    marginBottom: 28,
  },
});
