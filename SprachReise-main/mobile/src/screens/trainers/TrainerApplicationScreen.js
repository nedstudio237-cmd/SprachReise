import { useState } from 'react';
import {
  View, Text, StyleSheet, ScrollView, TextInput, TouchableOpacity,
  ActivityIndicator, Alert, KeyboardAvoidingView, Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as DocumentPicker from 'expo-document-picker';
import { COLORS, FONTS, LEVELS } from '../../constants/config';
import { trainerApplicationsAPI } from '../../services/api';

const BIO_MAX = 1000;
const MAX_PDF_BYTES = 5 * 1024 * 1024;

export default function TrainerApplicationScreen({ navigation }) {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [requestedLevelCode, setRequestedLevelCode] = useState('B1');
  const [nativeLanguage, setNativeLanguage] = useState('');
  const [motivation, setMotivation] = useState('');
  const [bio, setBio] = useState('');
  const [diploma, setDiploma] = useState(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(null); // { id }

  const [inviteToken, setInviteToken] = useState('');
  const [inviteLoading, setInviteLoading] = useState(false);
  const [inviteValid, setInviteValid] = useState(false);

  const checkInvitation = async () => {
    const t = inviteToken.trim();
    if (!t) {
      Alert.alert('Token requis', 'Colle le token figurant dans ton email d\'invitation.');
      return;
    }
    setInviteLoading(true);
    try {
      const res = await trainerApplicationsAPI.fromInvitation(t);
      if (res.data?.valid && res.data?.email) {
        setEmail(res.data.email);
        setInviteValid(true);
        Alert.alert('Invitation valide', `Email pré-rempli : ${res.data.email}`);
      } else {
        setInviteValid(false);
        Alert.alert('Invitation invalide', res.data?.error || 'Token introuvable');
      }
    } catch (e) {
      setInviteValid(false);
      Alert.alert(
        'Invitation invalide',
        e.response?.data?.error || e.message || 'Vérification impossible',
      );
    } finally {
      setInviteLoading(false);
    }
  };

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

  const submit = async () => {
    if (!firstName.trim() || !lastName.trim()) {
      Alert.alert('Champs requis', 'Prénom et nom sont obligatoires.');
      return;
    }
    if (!email.trim() || !email.includes('@')) {
      Alert.alert('Email invalide', 'Renseigne une adresse email valide.');
      return;
    }
    if (!requestedLevelCode) {
      Alert.alert('Niveau requis', 'Sélectionne un niveau souhaité.');
      return;
    }
    if (bio.length > BIO_MAX) {
      Alert.alert('Biographie trop longue', `Maximum ${BIO_MAX} caractères.`);
      return;
    }
    if (!diploma) {
      Alert.alert('Diplôme requis', 'Choisis ton diplôme en PDF.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await trainerApplicationsAPI.submit(
        {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          email: email.trim().toLowerCase(),
          phone: phone.trim(),
          bio: bio.trim(),
          requestedLevelCode,
          nativeLanguage: nativeLanguage.trim(),
          motivation: motivation.trim(),
        },
        diploma,
      );
      setSubmitted({ id: res.data?.id });
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Envoi impossible');
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <SafeAreaView style={styles.container}>
        <ScrollView contentContainerStyle={[styles.scroll, styles.center]}>
          <Text style={styles.successIcon}>✓</Text>
          <Text style={styles.successTitle}>Candidature envoyée</Text>
          <Text style={styles.successId}>N° de dossier : #{submitted.id}</Text>
          <Text style={styles.successText}>
            L'admin va examiner votre dossier dans les prochaines 48h. Vous recevrez
            un email à l'adresse fournie dès qu'une décision sera prise.
          </Text>
          <TouchableOpacity
            style={styles.primaryBtn}
            onPress={() => navigation.navigate('Login')}
          >
            <Text style={styles.primaryBtnText}>RETOUR À LA CONNEXION</Text>
          </TouchableOpacity>
        </ScrollView>
      </SafeAreaView>
    );
  }

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

          <Text style={styles.heading}>Devenir formateur</Text>
          <Text style={styles.intro}>
            Rejoignez SprachReise en tant qu'enseignant d'allemand. Remplissez ce
            formulaire ; l'équipe examine chaque candidature sous 48h.
          </Text>

          <View style={styles.inviteBox}>
            <Text style={styles.inviteTitle}>J'ai un lien d'invitation</Text>
            <Text style={styles.inviteHint}>
              Si l'admin vous a envoyé une invitation, collez le token reçu pour
              pré-remplir votre email.
            </Text>
            <View style={styles.inviteRow}>
              <TextInput
                style={[styles.input, styles.inviteInput]}
                value={inviteToken}
                onChangeText={setInviteToken}
                placeholder="Token d'invitation"
                placeholderTextColor="rgba(174,145,130,0.5)"
                autoCapitalize="none"
                autoCorrect={false}
              />
              <TouchableOpacity
                style={[styles.inviteBtn, inviteLoading && styles.primaryBtnDisabled]}
                onPress={checkInvitation}
                disabled={inviteLoading}
              >
                {inviteLoading ? (
                  <ActivityIndicator color={COLORS.parchment} size="small" />
                ) : (
                  <Text style={styles.inviteBtnText}>VÉRIFIER</Text>
                )}
              </TouchableOpacity>
            </View>
            {inviteValid && (
              <Text style={styles.inviteOk}>Token valide — email pré-rempli.</Text>
            )}
          </View>

          <Field label="Prénom" value={firstName} onChangeText={setFirstName} />
          <Field label="Nom" value={lastName} onChangeText={setLastName} />
          <Field
            label="Email"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
          />
          <Field
            label="Téléphone"
            value={phone}
            onChangeText={setPhone}
            keyboardType="phone-pad"
          />

          <View style={styles.field}>
            <Text style={styles.label}>Niveau souhaité</Text>
            <View style={styles.chipRow}>
              {LEVELS.map((lvl) => {
                const active = requestedLevelCode === lvl;
                return (
                  <TouchableOpacity
                    key={lvl}
                    style={[styles.chip, active && styles.chipActive]}
                    onPress={() => setRequestedLevelCode(lvl)}
                  >
                    <Text style={[styles.chipText, active && styles.chipTextActive]}>{lvl}</Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          </View>

          <Field
            label="Langue maternelle"
            value={nativeLanguage}
            onChangeText={setNativeLanguage}
            placeholder="Allemand, français…"
          />

          <View style={styles.field}>
            <Text style={styles.label}>Motivation</Text>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={motivation}
              onChangeText={setMotivation}
              multiline
              numberOfLines={4}
              placeholder="Pourquoi souhaitez-vous enseigner sur SprachReise ?"
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
          </View>

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
              numberOfLines={5}
              placeholder="Parcours, diplômes, spécialités…"
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Diplôme (PDF, max 5 Mo)</Text>
            <TouchableOpacity style={styles.pickerBtn} onPress={pickDiploma}>
              <Text style={styles.pickerBtnText}>
                {diploma ? diploma.name : 'Choisir le diplôme (PDF, max 5 Mo)'}
              </Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            style={[styles.primaryBtn, submitting && styles.primaryBtnDisabled]}
            onPress={submit}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={COLORS.parchment} />
            ) : (
              <Text style={styles.primaryBtnText}>ENVOYER MA CANDIDATURE</Text>
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
  center: { alignItems: 'center', justifyContent: 'center', flexGrow: 1 },

  backLink: { marginBottom: 8 },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },

  heading: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 26,
    marginBottom: 8,
  },
  intro: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 13,
    lineHeight: 19,
    marginBottom: 22,
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
  textarea: { minHeight: 100, textAlignVertical: 'top' },

  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.4)',
    backgroundColor: 'rgba(245,239,227,0.04)',
    marginRight: 8,
    marginBottom: 4,
  },
  chipActive: {
    backgroundColor: COLORS.accent,
    borderColor: COLORS.accent,
  },
  chipText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.muted,
    fontSize: 12,
    letterSpacing: 1,
  },
  chipTextActive: { color: COLORS.parchment },

  pickerBtn: {
    backgroundColor: 'rgba(184,137,58,0.12)',
    borderWidth: 1,
    borderColor: COLORS.gold,
    borderRadius: 8,
    paddingVertical: 14,
    paddingHorizontal: 14,
    alignItems: 'center',
  },
  pickerBtnText: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.gold,
    fontSize: 13,
  },

  primaryBtn: {
    backgroundColor: COLORS.accent,
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 16,
  },
  primaryBtnDisabled: { opacity: 0.6 },
  primaryBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 13,
    letterSpacing: 1.2,
  },

  successIcon: {
    fontFamily: FONTS.displayBold,
    color: COLORS.gold,
    fontSize: 64,
    marginBottom: 12,
  },
  successTitle: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 26,
    marginBottom: 8,
  },
  successId: {
    fontFamily: FONTS.uiBold,
    color: COLORS.gold,
    fontSize: 14,
    letterSpacing: 1.2,
    marginBottom: 14,
  },
  successText: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 14,
    lineHeight: 21,
    textAlign: 'center',
    marginBottom: 28,
    paddingHorizontal: 12,
  },

  inviteBox: {
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.4)',
    borderRadius: 10,
    padding: 12,
    marginBottom: 20,
    backgroundColor: 'rgba(184,137,58,0.06)',
  },
  inviteTitle: {
    fontFamily: FONTS.uiBold,
    color: COLORS.gold,
    fontSize: 12,
    letterSpacing: 1.2,
    marginBottom: 4,
  },
  inviteHint: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 12,
    lineHeight: 17,
    marginBottom: 10,
  },
  inviteRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  inviteInput: {
    flex: 1,
    marginRight: 8,
  },
  inviteBtn: {
    backgroundColor: COLORS.gold,
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderRadius: 8,
    minWidth: 90,
    alignItems: 'center',
  },
  inviteBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 12,
    letterSpacing: 1,
  },
  inviteOk: {
    marginTop: 8,
    fontFamily: FONTS.uiMedium,
    color: COLORS.gold,
    fontSize: 12,
  },
});
