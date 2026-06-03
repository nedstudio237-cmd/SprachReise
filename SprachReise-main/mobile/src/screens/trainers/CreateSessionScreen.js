import { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Switch,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as DocumentPicker from 'expo-document-picker';
import { COLORS, FONTS } from '../../constants/config';
import { sessionsAPI } from '../../services/api';

export default function CreateSessionScreen({ navigation, route }) {
  const existing = route?.params?.session || null;
  const isEdit = !!existing;

  const [title, setTitle] = useState(existing?.title || '');
  const [description, setDescription] = useState(existing?.description || '');
  const [scheduledStart, setScheduledStart] = useState(existing?.scheduledStart || '');
  const [durationMinutes, setDurationMinutes] = useState(
    existing?.durationMinutes ? String(existing.durationMinutes) : '60'
  );
  const [recordEnabled, setRecordEnabled] = useState(!!existing?.recordEnabled);
  const [pdfFile, setPdfFile] = useState(null);
  const [pdfPath, setPdfPath] = useState(existing?.attachmentPdf || null);
  const [submitting, setSubmitting] = useState(false);

  const pickPdf = async () => {
    try {
      const res = await DocumentPicker.getDocumentAsync({
        type: 'application/pdf',
        copyToCacheDirectory: true,
        multiple: false,
      });
      if (res.canceled || !res.assets?.[0]) return;
      const a = res.assets[0];
      if (a.size && a.size > 20 * 1024 * 1024) {
        Alert.alert('PDF trop volumineux', 'Maximum 20 Mo.');
        return;
      }
      const name = a.name || `session_${Date.now()}.pdf`;
      setPdfFile({ uri: a.uri, name, type: 'application/pdf', size: a.size });
    } catch (e) {
      Alert.alert('Erreur', e.message || 'Sélection impossible');
    }
  };

  const submit = async () => {
    if (!title.trim()) { Alert.alert('Titre requis'); return; }
    if (!scheduledStart.trim() || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(scheduledStart.trim())) {
      Alert.alert('Date invalide', 'Format ISO attendu : 2026-06-01T18:30');
      return;
    }
    const dm = parseInt(durationMinutes, 10);
    if (!dm || dm <= 0) { Alert.alert('Durée invalide'); return; }

    setSubmitting(true);
    try {
      let sessionId = existing?.id;
      const payload = {
        title: title.trim(),
        description: description.trim(),
        scheduledStart: scheduledStart.trim(),
        durationMinutes: dm,
        recordEnabled,
      };
      if (pdfPath && !pdfFile) payload.attachmentPdf = pdfPath;

      if (isEdit) {
        await sessionsAPI.update(sessionId, payload);
      } else {
        const res = await sessionsAPI.create(payload);
        sessionId = res.data?.id;
      }

      if (pdfFile && sessionId) {
        await sessionsAPI.uploadAttachment(sessionId, pdfFile.uri, pdfFile.name, pdfFile.type);
      }

      Alert.alert(
        isEdit ? 'Session mise à jour' : 'Session programmée',
        '',
        [{ text: 'OK', onPress: () => navigation.goBack() }]
      );
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Échec');
    } finally {
      setSubmitting(false);
    }
  };

  const cancelSession = () => {
    if (!isEdit) return;
    Alert.alert(
      'Annuler la session ?',
      `« ${existing.title} » sera annulée.`,
      [
        { text: 'Non', style: 'cancel' },
        {
          text: 'Annuler',
          style: 'destructive',
          onPress: async () => {
            try {
              await sessionsAPI.cancel(existing.id);
              navigation.goBack();
            } catch (e) {
              Alert.alert('Erreur', e.response?.data?.error || e.message);
            }
          },
        },
      ]
    );
  };

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

          <Text style={styles.heading}>{isEdit ? 'Modifier la session' : 'Nouvelle session live'}</Text>

          <Field label="Titre" value={title} onChangeText={setTitle} placeholder="Titre de la session" />

          <View style={styles.field}>
            <Text style={styles.label}>Description</Text>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={description}
              onChangeText={setDescription}
              multiline
              numberOfLines={4}
              placeholder="Présente le programme de la session"
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
          </View>

          <Field
            label="Date et heure (ISO)"
            value={scheduledStart}
            onChangeText={setScheduledStart}
            placeholder="2026-06-01T18:30"
            autoCapitalize="none"
          />

          <Field
            label="Durée (minutes)"
            value={durationMinutes}
            onChangeText={setDurationMinutes}
            placeholder="60"
            keyboardType="number-pad"
          />

          <View style={styles.statusRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.label}>Enregistrer la session</Text>
              <Text style={styles.statusHint}>
                {recordEnabled ? 'OUI — un fichier .mp4 sera créé à la fin' : 'NON — pas d\'enregistrement'}
              </Text>
            </View>
            <Switch
              value={recordEnabled}
              onValueChange={setRecordEnabled}
              trackColor={{ false: 'rgba(245,239,227,0.2)', true: COLORS.accent }}
              thumbColor={recordEnabled ? COLORS.gold : COLORS.muted}
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Support PDF (optionnel, max 20 Mo)</Text>
            <TouchableOpacity style={styles.fileBtn} onPress={pickPdf}>
              <Text style={styles.fileBtnText}>
                {pdfFile ? `📄 ${pdfFile.name}` : (pdfPath ? `📄 ${pdfPath}` : 'Choisir un PDF')}
              </Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            style={[styles.saveBtn, submitting && styles.saveBtnDisabled]}
            onPress={submit}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={COLORS.parchment} />
            ) : (
              <Text style={styles.saveBtnText}>
                {isEdit ? 'ENREGISTRER' : 'PROGRAMMER LA SESSION'}
              </Text>
            )}
          </TouchableOpacity>

          {isEdit && existing.status === 'SCHEDULED' && (
            <TouchableOpacity style={styles.cancelBtn} onPress={cancelSession}>
              <Text style={styles.cancelBtnText}>ANNULER LA SESSION</Text>
            </TouchableOpacity>
          )}
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
  scroll: { padding: 20, paddingBottom: 60 },

  backLink: { marginBottom: 8 },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },

  heading: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 24,
    marginBottom: 18,
  },

  field: { marginBottom: 16 },
  label: {
    fontFamily: FONTS.uiBold,
    color: COLORS.muted,
    fontSize: 11,
    letterSpacing: 1.2,
    marginBottom: 6,
  },
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
  textarea: { minHeight: 90, textAlignVertical: 'top' },

  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
    gap: 10,
  },
  statusHint: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 12,
  },

  fileBtn: {
    backgroundColor: 'rgba(184,137,58,0.12)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.4)',
    paddingVertical: 14,
    paddingHorizontal: 14,
    alignItems: 'center',
  },
  fileBtnText: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.gold,
    fontSize: 13,
  },

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

  cancelBtn: {
    marginTop: 14,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(239,68,68,0.4)',
    backgroundColor: 'rgba(239,68,68,0.08)',
  },
  cancelBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.error,
    fontSize: 12,
    letterSpacing: 1,
  },
});
