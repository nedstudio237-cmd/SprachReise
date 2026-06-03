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
import { COLORS, FONTS } from '../../constants/config';
import { examsAPI, trainersAPI } from '../../services/api';

export default function MyExamEditorScreen({ navigation, route }) {
  const existing = route?.params?.exam || null;
  const isEdit = !!existing;

  const [loadingLevel, setLoadingLevel] = useState(!isEdit);
  const [lockedLevel, setLockedLevel] = useState(isEdit ? (existing.levelCode || '?') : '?');
  const [title, setTitle] = useState(existing?.title || '');
  const [instructions, setInstructions] = useState(existing?.instructions || '');
  const [scheduledAt, setScheduledAt] = useState(existing?.scheduledAt || '');
  const [statusPublished, setStatusPublished] = useState(existing?.status === 'PUBLISHED');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isEdit) return;
    (async () => {
      try {
        const res = await trainersAPI.getMe();
        setLockedLevel(res.data?.profile?.levelCode || '?');
      } catch (e) {
        Alert.alert('Erreur', e.response?.data?.error || 'Niveau introuvable');
      } finally {
        setLoadingLevel(false);
      }
    })();
  }, [isEdit]);

  const submit = async () => {
    if (!title.trim()) { Alert.alert('Titre requis'); return; }
    if (scheduledAt && !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(scheduledAt.trim())) {
      Alert.alert('Date invalide', 'Format ISO : 2026-06-01T18:30');
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        title: title.trim(),
        instructions: instructions.trim(),
        scheduledAt: scheduledAt.trim() || null,
        status: statusPublished ? 'PUBLISHED' : 'DRAFT',
      };
      if (isEdit) {
        await examsAPI.update(existing.id, payload);
        Alert.alert('Épreuve mise à jour', '', [{ text: 'OK', onPress: () => navigation.goBack() }]);
      } else {
        await examsAPI.create(payload);
        Alert.alert('Épreuve créée', '', [{ text: 'OK', onPress: () => navigation.goBack() }]);
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

          <Text style={styles.heading}>{isEdit ? 'Modifier l\'épreuve' : 'Nouvelle épreuve'}</Text>

          <View style={styles.lockedBox}>
            <Text style={styles.lockedLabel}>NIVEAU</Text>
            <Text style={styles.lockedValue}>🔒 Verrouillé sur votre niveau {lockedLevel}</Text>
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Titre</Text>
            <TextInput
              style={styles.input}
              value={title}
              onChangeText={setTitle}
              placeholder="Titre de l'épreuve"
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Consignes</Text>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={instructions}
              onChangeText={setInstructions}
              multiline
              numberOfLines={6}
              placeholder="Décris l'épreuve, le format attendu, le temps imparti..."
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
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
            <Text style={styles.label}>Date programmée (optionnel)</Text>
            <TextInput
              style={styles.input}
              value={scheduledAt}
              onChangeText={setScheduledAt}
              placeholder="2026-06-01T18:30"
              placeholderTextColor="rgba(174,145,130,0.5)"
              autoCapitalize="none"
            />
          </View>

          <TouchableOpacity
            style={[styles.saveBtn, submitting && styles.saveBtnDisabled]}
            onPress={submit}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={COLORS.parchment} />
            ) : (
              <Text style={styles.saveBtnText}>{isEdit ? 'ENREGISTRER' : 'CRÉER L\'ÉPREUVE'}</Text>
            )}
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },
  scroll: { padding: 20, paddingBottom: 60 },
  center: { alignItems: 'center', justifyContent: 'center' },

  backLink: { marginBottom: 8 },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },

  heading: { fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 24, marginBottom: 18 },

  lockedBox: {
    backgroundColor: 'rgba(184,137,58,0.12)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.35)',
    padding: 12,
    marginBottom: 18,
  },
  lockedLabel: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 10, letterSpacing: 1.4, marginBottom: 4 },
  lockedValue: { fontFamily: FONTS.uiMedium, color: COLORS.parchment, fontSize: 14 },

  field: { marginBottom: 16 },
  label: { fontFamily: FONTS.uiBold, color: COLORS.muted, fontSize: 11, letterSpacing: 1.2, marginBottom: 6 },
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
  textarea: { minHeight: 130, textAlignVertical: 'top' },

  statusRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 16, gap: 10 },
  statusHint: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 12 },

  saveBtn: { backgroundColor: COLORS.accent, paddingVertical: 14, borderRadius: 8, alignItems: 'center', marginTop: 12 },
  saveBtnDisabled: { opacity: 0.6 },
  saveBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 13, letterSpacing: 1.2 },
});
