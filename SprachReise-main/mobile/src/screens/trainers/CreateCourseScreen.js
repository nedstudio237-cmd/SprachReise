import { useEffect, useState } from 'react';
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
import { coursesAPI, trainersAPI } from '../../services/api';

const THEMES = ['Grammaire', 'Vocabulaire', 'Communication', 'Culture', 'Voyage', 'Affaires'];

export default function CreateCourseScreen({ navigation, route }) {
  const existing = route?.params?.course || null;
  const isEdit = !!existing;

  const [loadingLevel, setLoadingLevel] = useState(!isEdit);
  const [lockedLevel, setLockedLevel] = useState(isEdit ? (existing.levelCode || '?') : '?');
  const [title, setTitle] = useState(existing?.title || '');
  const [description, setDescription] = useState(existing?.description || '');
  const [theme, setTheme] = useState(existing?.theme || '');
  const [statusPublished, setStatusPublished] = useState(existing?.status === 'PUBLISHED');
  const [publishAt, setPublishAt] = useState(existing?.publishAt || '');
  const [videoFile, setVideoFile] = useState(null);
  const [pdfFile, setPdfFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isEdit) return;
    (async () => {
      try {
        const res = await trainersAPI.getMe();
        const code = res.data?.profile?.levelCode || '?';
        setLockedLevel(code);
      } catch (e) {
        Alert.alert('Erreur', e.response?.data?.error || 'Niveau introuvable');
      } finally {
        setLoadingLevel(false);
      }
    })();
  }, [isEdit]);

  const pickVideo = async () => {
    try {
      const res = await DocumentPicker.getDocumentAsync({
        type: ['video/mp4', 'video/quicktime'],
        copyToCacheDirectory: true,
        multiple: false,
      });
      if (res.canceled || !res.assets?.[0]) return;
      const a = res.assets[0];
      if (a.size && a.size > 500 * 1024 * 1024) {
        Alert.alert('Vidéo trop volumineuse', 'Maximum 500 Mo.');
        return;
      }
      const name = a.name || `video_${Date.now()}.mp4`;
      const lower = name.toLowerCase();
      const type = lower.endsWith('.mov') ? 'video/quicktime' : 'video/mp4';
      setVideoFile({ uri: a.uri, name, type, size: a.size });
    } catch (e) {
      Alert.alert('Erreur', e.message || 'Sélection impossible');
    }
  };

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
      const name = a.name || `support_${Date.now()}.pdf`;
      setPdfFile({ uri: a.uri, name, type: 'application/pdf', size: a.size });
    } catch (e) {
      Alert.alert('Erreur', e.message || 'Sélection impossible');
    }
  };

  const submit = async () => {
    if (!title.trim()) { Alert.alert('Titre requis'); return; }
    if (!description.trim()) { Alert.alert('Description requise'); return; }
    if (!isEdit && !pdfFile) { Alert.alert('PDF requis', 'Sélectionne le support PDF.'); return; }
    if (publishAt && !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(publishAt.trim())) {
      Alert.alert('Date invalide', 'Format ISO attendu : 2026-06-01T18:30');
      return;
    }

    setSubmitting(true);
    try {
      if (isEdit) {
        const payload = {
          title: title.trim(),
          description: description.trim(),
          theme: theme || '',
          status: statusPublished ? 'PUBLISHED' : 'DRAFT',
          publishAt: publishAt.trim() || null,
        };
        await coursesAPI.update(existing.id, payload);
        Alert.alert('Cours mis à jour', '', [
          { text: 'OK', onPress: () => navigation.goBack() },
        ]);
      } else {
        await coursesAPI.create({
          title: title.trim(),
          description: description.trim(),
          theme: theme || '',
          status: statusPublished ? 'PUBLISHED' : 'DRAFT',
          publishAt: publishAt.trim() || '',
          videoFile,
          pdfFile,
        });
        Alert.alert('Cours créé', 'Ton cours a été enregistré.', [
          { text: 'OK', onPress: () => navigation.goBack() },
        ]);
      }
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Échec');
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingLevel) {
    return (
      <SafeAreaView style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color={COLORS.gold} />
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

          <Text style={styles.heading}>{isEdit ? 'Modifier le cours' : 'Nouveau cours'}</Text>

          <View style={styles.lockedBox}>
            <Text style={styles.lockedLabel}>NIVEAU</Text>
            <Text style={styles.lockedValue}>
              🔒 Verrouillé sur votre niveau {lockedLevel}
            </Text>
          </View>

          <Field label="Titre" value={title} onChangeText={setTitle} placeholder="Titre du cours" />

          <View style={styles.field}>
            <Text style={styles.label}>Description</Text>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={description}
              onChangeText={setDescription}
              multiline
              numberOfLines={5}
              placeholder="Présente le contenu du cours"
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Thème</Text>
            <View style={styles.chipsRow}>
              {THEMES.map((t) => {
                const active = theme === t;
                return (
                  <TouchableOpacity
                    key={t}
                    style={[styles.chip, active && styles.chipActive]}
                    onPress={() => setTheme(active ? '' : t)}
                  >
                    <Text style={[styles.chipText, active && styles.chipTextActive]}>{t}</Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          </View>

          <View style={styles.statusRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.label}>Statut</Text>
              <Text style={styles.statusHint}>
                {statusPublished ? 'PUBLISHED — visible des apprenants' : 'DRAFT — brouillon'}
              </Text>
            </View>
            <Switch
              value={statusPublished}
              onValueChange={setStatusPublished}
              trackColor={{ false: 'rgba(245,239,227,0.2)', true: COLORS.accent }}
              thumbColor={statusPublished ? COLORS.gold : COLORS.muted}
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Publication programmée (optionnel)</Text>
            <TextInput
              style={styles.input}
              value={publishAt}
              onChangeText={setPublishAt}
              placeholder="2026-06-01T18:30"
              placeholderTextColor="rgba(174,145,130,0.5)"
              autoCapitalize="none"
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Vidéo {isEdit ? '(inchangée en modification)' : '(MP4/MOV, max 500 Mo)'}</Text>
            {isEdit ? (
              <View style={[styles.fileBtn, { opacity: 0.6 }]}>
                <Text style={styles.fileBtnText}>Vidéo non modifiable ici</Text>
              </View>
            ) : (
              <TouchableOpacity style={styles.fileBtn} onPress={pickVideo}>
                <Text style={styles.fileBtnText}>
                  {videoFile ? `🎬 ${videoFile.name}` : 'Choisir la vidéo'}
                </Text>
              </TouchableOpacity>
            )}
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>PDF {isEdit ? '(inchangé en modification)' : '(max 20 Mo, requis)'}</Text>
            {isEdit ? (
              <View style={[styles.fileBtn, { opacity: 0.6 }]}>
                <Text style={styles.fileBtnText}>PDF non modifiable ici</Text>
              </View>
            ) : (
              <TouchableOpacity style={styles.fileBtn} onPress={pickPdf}>
                <Text style={styles.fileBtnText}>
                  {pdfFile ? `📄 ${pdfFile.name}` : 'Choisir le PDF'}
                </Text>
              </TouchableOpacity>
            )}
          </View>

          <TouchableOpacity
            style={[styles.saveBtn, submitting && styles.saveBtnDisabled]}
            onPress={submit}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={COLORS.parchment} />
            ) : (
              <Text style={styles.saveBtnText}>{isEdit ? 'ENREGISTRER' : 'CRÉER LE COURS'}</Text>
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
  scroll: { padding: 20, paddingBottom: 60 },
  center: { alignItems: 'center', justifyContent: 'center' },

  backLink: { marginBottom: 8 },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },

  heading: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 24,
    marginBottom: 18,
  },

  lockedBox: {
    backgroundColor: 'rgba(184,137,58,0.12)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.35)',
    padding: 12,
    marginBottom: 18,
  },
  lockedLabel: {
    fontFamily: FONTS.uiBold,
    color: COLORS.gold,
    fontSize: 10,
    letterSpacing: 1.4,
    marginBottom: 4,
  },
  lockedValue: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.parchment,
    fontSize: 14,
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
  textarea: { minHeight: 110, textAlignVertical: 'top' },

  chipsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.4)',
    backgroundColor: 'rgba(245,239,227,0.04)',
  },
  chipActive: {
    backgroundColor: COLORS.accent,
    borderColor: COLORS.gold,
  },
  chipText: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.cream,
    fontSize: 12,
  },
  chipTextActive: {
    color: COLORS.parchment,
    fontFamily: FONTS.uiBold,
  },

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
});
