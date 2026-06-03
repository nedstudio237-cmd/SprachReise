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
import { qcmsAPI, trainersAPI } from '../../services/api';

const MIN_Q = 5;
const MAX_Q = 30;
const THEMES = ['Grammaire', 'Vocabulaire', 'Communication', 'Culture', 'Voyage', 'Affaires'];

const newChoice = () => ({ text: '', isCorrect: false, explanation: '' });
const newQuestion = () => ({
  text: '',
  type: 'SINGLE_CHOICE',
  choices: [newChoice(), newChoice()],
});

export default function MyQcmEditorScreen({ navigation, route }) {
  const existing = route?.params?.qcm || null;
  const isEdit = !!existing;

  const [loadingLevel, setLoadingLevel] = useState(!isEdit);
  const [lockedLevel, setLockedLevel] = useState('?');
  const [title, setTitle] = useState(existing?.title || '');
  const [theme, setTheme] = useState(existing?.theme || '');
  const [statusPublished, setStatusPublished] = useState(existing?.status === 'PUBLISHED');
  const [scheduledAt, setScheduledAt] = useState(existing?.scheduledAt || '');
  const [questions, setQuestions] = useState(() => {
    if (existing?.questions && existing.questions.length > 0) {
      return existing.questions.map((q) => ({
        text: q.questionText || '',
        type: q.questionType || 'SINGLE_CHOICE',
        choices: (q.choices || []).map((c) => ({
          text: c.choiceText || '',
          isCorrect: !!c.isCorrect,
          explanation: c.explanation || '',
        })),
      }));
    }
    return Array.from({ length: MIN_Q }, newQuestion);
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const res = await trainersAPI.getMe();
        setLockedLevel(res.data?.profile?.levelCode || '?');
      } catch (e) {
        if (!isEdit) Alert.alert('Erreur', e.response?.data?.error || 'Niveau introuvable');
      } finally {
        setLoadingLevel(false);
      }
    })();
  }, [isEdit]);

  const setQuestion = (i, updater) => {
    setQuestions((qs) => qs.map((q, idx) => (idx === i ? updater(q) : q)));
  };

  const addQuestion = () => {
    if (questions.length >= MAX_Q) {
      Alert.alert('Limite atteinte', `Maximum ${MAX_Q} questions.`);
      return;
    }
    setQuestions((qs) => [...qs, newQuestion()]);
  };

  const removeQuestion = (i) => {
    if (questions.length <= 1) return;
    setQuestions((qs) => qs.filter((_, idx) => idx !== i));
  };

  const addChoice = (i) => {
    setQuestion(i, (q) => ({ ...q, choices: [...q.choices, newChoice()] }));
  };

  const removeChoice = (i, ci) => {
    setQuestion(i, (q) => {
      if (q.choices.length <= 2) return q;
      return { ...q, choices: q.choices.filter((_, idx) => idx !== ci) };
    });
  };

  const toggleCorrect = (i, ci) => {
    setQuestion(i, (q) => {
      const single = q.type === 'SINGLE_CHOICE';
      return {
        ...q,
        choices: q.choices.map((c, idx) =>
          single
            ? { ...c, isCorrect: idx === ci }
            : idx === ci ? { ...c, isCorrect: !c.isCorrect } : c
        ),
      };
    });
  };

  const setType = (i, type) => {
    setQuestion(i, (q) => {
      if (type === 'SINGLE_CHOICE') {
        // keep only the first correct choice
        let kept = false;
        return {
          ...q,
          type,
          choices: q.choices.map((c) => {
            if (c.isCorrect && !kept) { kept = true; return c; }
            return { ...c, isCorrect: false };
          }),
        };
      }
      return { ...q, type };
    });
  };

  const validate = () => {
    if (!title.trim()) return 'Titre requis';
    if (questions.length < MIN_Q || questions.length > MAX_Q) {
      return `Entre ${MIN_Q} et ${MAX_Q} questions requises (actuellement ${questions.length})`;
    }
    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.text.trim()) return `Question #${i + 1} : texte requis`;
      if (!q.choices || q.choices.length < 2) return `Question #${i + 1} : au moins 2 choix`;
      const correctCount = q.choices.filter((c) => c.isCorrect).length;
      if (correctCount < 1) return `Question #${i + 1} : au moins une bonne réponse`;
      for (let j = 0; j < q.choices.length; j++) {
        if (!q.choices[j].text.trim()) return `Question #${i + 1}, choix #${j + 1} : texte requis`;
      }
    }
    if (scheduledAt && !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(scheduledAt.trim())) {
      return 'Date invalide (format : 2026-06-01T18:30)';
    }
    return null;
  };

  const submit = async () => {
    const err = validate();
    if (err) { Alert.alert('Validation', err); return; }

    setSubmitting(true);
    try {
      const payload = {
        title: title.trim(),
        theme: theme || null,
        scheduledAt: scheduledAt.trim() || null,
        status: statusPublished ? 'PUBLISHED' : 'DRAFT',
        questions: questions.map((q) => ({
          text: q.text.trim(),
          type: q.type,
          choices: q.choices.map((c) => ({
            text: c.text.trim(),
            isCorrect: !!c.isCorrect,
            explanation: c.explanation ? c.explanation.trim() : null,
          })),
        })),
      };
      if (isEdit) {
        await qcmsAPI.update(existing.id, payload);
        Alert.alert('QCM mis à jour', '', [{ text: 'OK', onPress: () => navigation.goBack() }]);
      } else {
        await qcmsAPI.create(payload);
        Alert.alert('QCM créé', 'Votre QCM a été enregistré.', [
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

          <Text style={styles.heading}>{isEdit ? 'Modifier le QCM' : 'Nouveau QCM'}</Text>

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
              placeholder="Titre du QCM"
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

          <View style={styles.sectionHeadRow}>
            <Text style={styles.sectionTitle}>
              Questions ({questions.length}/{MAX_Q})
            </Text>
            <TouchableOpacity style={styles.addBtn} onPress={addQuestion}>
              <Text style={styles.addBtnText}>+ AJOUTER</Text>
            </TouchableOpacity>
          </View>
          <Text style={styles.helper}>
            Minimum {MIN_Q}, maximum {MAX_Q}. Au moins 2 choix par question et 1 bonne réponse.
          </Text>

          {questions.map((q, i) => (
            <View key={i} style={styles.qCard}>
              <View style={styles.qHead}>
                <Text style={styles.qIndex}>QUESTION {i + 1}</Text>
                <TouchableOpacity onPress={() => removeQuestion(i)} disabled={questions.length <= 1}>
                  <Text style={[styles.qRemove, questions.length <= 1 && { opacity: 0.3 }]}>Supprimer</Text>
                </TouchableOpacity>
              </View>

              <TextInput
                style={[styles.input, styles.textarea]}
                value={q.text}
                onChangeText={(v) => setQuestion(i, (qq) => ({ ...qq, text: v }))}
                multiline
                placeholder="Énoncé de la question"
                placeholderTextColor="rgba(174,145,130,0.5)"
              />

              <View style={styles.typeRow}>
                <TouchableOpacity
                  style={[styles.typeChip, q.type === 'SINGLE_CHOICE' && styles.typeChipActive]}
                  onPress={() => setType(i, 'SINGLE_CHOICE')}
                >
                  <Text style={[styles.typeChipText, q.type === 'SINGLE_CHOICE' && styles.typeChipTextActive]}>
                    Choix unique
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.typeChip, q.type === 'MULTI_CHOICE' && styles.typeChipActive]}
                  onPress={() => setType(i, 'MULTI_CHOICE')}
                >
                  <Text style={[styles.typeChipText, q.type === 'MULTI_CHOICE' && styles.typeChipTextActive]}>
                    Choix multiple
                  </Text>
                </TouchableOpacity>
              </View>

              {q.choices.map((c, ci) => (
                <View key={ci} style={styles.choiceRow}>
                  <TouchableOpacity
                    style={[styles.correctMark, c.isCorrect && styles.correctMarkOn]}
                    onPress={() => toggleCorrect(i, ci)}
                  >
                    <Text style={styles.correctMarkText}>{c.isCorrect ? '✓' : ''}</Text>
                  </TouchableOpacity>
                  <View style={{ flex: 1 }}>
                    <TextInput
                      style={styles.input}
                      value={c.text}
                      onChangeText={(v) =>
                        setQuestion(i, (qq) => ({
                          ...qq,
                          choices: qq.choices.map((cc, idx) => (idx === ci ? { ...cc, text: v } : cc)),
                        }))
                      }
                      placeholder={`Choix ${ci + 1}`}
                      placeholderTextColor="rgba(174,145,130,0.5)"
                    />
                    <TextInput
                      style={[styles.input, styles.explanationInput]}
                      value={c.explanation}
                      onChangeText={(v) =>
                        setQuestion(i, (qq) => ({
                          ...qq,
                          choices: qq.choices.map((cc, idx) => (idx === ci ? { ...cc, explanation: v } : cc)),
                        }))
                      }
                      placeholder="Explication (optionnel)"
                      placeholderTextColor="rgba(174,145,130,0.5)"
                    />
                  </View>
                  <TouchableOpacity onPress={() => removeChoice(i, ci)} disabled={q.choices.length <= 2}>
                    <Text style={[styles.removeChoice, q.choices.length <= 2 && { opacity: 0.3 }]}>✕</Text>
                  </TouchableOpacity>
                </View>
              ))}

              <TouchableOpacity style={styles.addChoiceBtn} onPress={() => addChoice(i)}>
                <Text style={styles.addChoiceText}>+ Ajouter un choix</Text>
              </TouchableOpacity>
            </View>
          ))}

          <TouchableOpacity
            style={[styles.saveBtn, submitting && styles.saveBtnDisabled]}
            onPress={submit}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={COLORS.parchment} />
            ) : (
              <Text style={styles.saveBtnText}>{isEdit ? 'ENREGISTRER' : 'CRÉER LE QCM'}</Text>
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
    paddingVertical: 10,
    fontFamily: FONTS.regular,
    color: COLORS.parchment,
    fontSize: 14,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
  },
  textarea: { minHeight: 70, textAlignVertical: 'top', marginBottom: 10 },
  explanationInput: { marginTop: 6, fontSize: 12, paddingVertical: 8 },

  chipsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingHorizontal: 12, paddingVertical: 7, borderRadius: 16, borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.4)', backgroundColor: 'rgba(245,239,227,0.04)',
  },
  chipActive: { backgroundColor: COLORS.accent, borderColor: COLORS.gold },
  chipText: { fontFamily: FONTS.uiMedium, color: COLORS.cream, fontSize: 12 },
  chipTextActive: { color: COLORS.parchment, fontFamily: FONTS.uiBold },

  statusRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 16, gap: 10 },
  statusHint: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 12 },

  sectionHeadRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 6, marginBottom: 6 },
  sectionTitle: { fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 16 },
  addBtn: { backgroundColor: COLORS.accent, paddingHorizontal: 12, paddingVertical: 7, borderRadius: 6 },
  addBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 11, letterSpacing: 1 },
  helper: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 12, marginBottom: 12 },

  qCard: {
    backgroundColor: 'rgba(245,239,227,0.05)',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    padding: 12,
    marginBottom: 14,
  },
  qHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  qIndex: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 11, letterSpacing: 1.2 },
  qRemove: { fontFamily: FONTS.uiBold, color: COLORS.error, fontSize: 11, letterSpacing: 1 },

  typeRow: { flexDirection: 'row', gap: 8, marginBottom: 12 },
  typeChip: {
    paddingHorizontal: 10, paddingVertical: 6, borderRadius: 14, borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.4)', backgroundColor: 'rgba(245,239,227,0.04)',
  },
  typeChipActive: { backgroundColor: COLORS.gold, borderColor: COLORS.gold },
  typeChipText: { fontFamily: FONTS.uiMedium, color: COLORS.cream, fontSize: 11 },
  typeChipTextActive: { color: COLORS.deep, fontFamily: FONTS.uiBold },

  choiceRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 8, marginBottom: 10 },
  correctMark: {
    width: 28, height: 28, borderRadius: 14, borderWidth: 1.5,
    borderColor: COLORS.muted, alignItems: 'center', justifyContent: 'center', marginTop: 8,
  },
  correctMarkOn: { backgroundColor: COLORS.success, borderColor: COLORS.success },
  correctMarkText: { color: COLORS.parchment, fontFamily: FONTS.uiBold, fontSize: 14 },
  removeChoice: { color: COLORS.error, fontSize: 18, paddingHorizontal: 6, paddingTop: 8 },

  addChoiceBtn: {
    paddingVertical: 8, alignItems: 'center', borderRadius: 6,
    borderWidth: 1, borderColor: 'rgba(184,137,58,0.4)', backgroundColor: 'rgba(184,137,58,0.08)',
  },
  addChoiceText: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 11, letterSpacing: 1 },

  saveBtn: { backgroundColor: COLORS.accent, paddingVertical: 14, borderRadius: 8, alignItems: 'center', marginTop: 16 },
  saveBtnDisabled: { opacity: 0.6 },
  saveBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 13, letterSpacing: 1.2 },
});
