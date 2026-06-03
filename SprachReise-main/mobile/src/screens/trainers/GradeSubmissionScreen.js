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
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { COLORS, FONTS } from '../../constants/config';
import { examsAPI } from '../../services/api';

export default function GradeSubmissionScreen({ navigation, route }) {
  const { submission, exam } = route.params || {};
  const [grade, setGrade] = useState(submission?.grade != null ? String(submission.grade) : '');
  const [feedback, setFeedback] = useState(submission?.feedback || '');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    const num = Number(String(grade).replace(',', '.'));
    if (!Number.isFinite(num) || num < 0 || num > 20) {
      Alert.alert('Note invalide', 'Saisis une note entre 0 et 20.');
      return;
    }
    setSubmitting(true);
    try {
      await examsAPI.grade(submission.id, num, feedback.trim());
      Alert.alert('Copie corrigée', 'L\'apprenant a été notifié.', [
        { text: 'OK', onPress: () => navigation.goBack() },
      ]);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Échec');
    } finally {
      setSubmitting(false);
    }
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

          <Text style={styles.heading}>Corriger la copie</Text>
          <Text style={styles.subHeading}>{exam?.title}</Text>

          <View style={styles.metaBox}>
            <Text style={styles.metaLabel}>APPRENANT</Text>
            <Text style={styles.metaValue}>{submission?.learnerName}</Text>
            {submission?.learnerEmail ? (
              <Text style={styles.metaHint}>{submission.learnerEmail}</Text>
            ) : null}
          </View>

          <Text style={styles.label}>Réponse de l'apprenant</Text>
          <View style={styles.answerBox}>
            <Text style={styles.answerText}>{submission?.answerText || '(vide)'}</Text>
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Note /20</Text>
            <TextInput
              style={styles.input}
              value={grade}
              onChangeText={setGrade}
              keyboardType="decimal-pad"
              placeholder="Ex. 14.5"
              placeholderTextColor="rgba(174,145,130,0.5)"
            />
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Commentaire</Text>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={feedback}
              onChangeText={setFeedback}
              multiline
              numberOfLines={6}
              placeholder="Feedback pour l'apprenant"
              placeholderTextColor="rgba(174,145,130,0.5)"
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
              <Text style={styles.saveBtnText}>ENVOYER LA CORRECTION</Text>
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

  backLink: { marginBottom: 8 },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },

  heading: { fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 24, marginBottom: 4 },
  subHeading: { fontFamily: FONTS.ui, color: COLORS.gold, fontSize: 13, marginBottom: 16 },

  metaBox: {
    backgroundColor: 'rgba(184,137,58,0.1)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.3)',
    padding: 12,
    marginBottom: 18,
  },
  metaLabel: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 10, letterSpacing: 1.4, marginBottom: 4 },
  metaValue: { fontFamily: FONTS.uiMedium, color: COLORS.parchment, fontSize: 14 },
  metaHint: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 12, marginTop: 2 },

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
  textarea: { minHeight: 110, textAlignVertical: 'top' },

  answerBox: {
    backgroundColor: 'rgba(245,239,227,0.04)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    padding: 12,
    marginBottom: 18,
  },
  answerText: { fontFamily: FONTS.regular, color: COLORS.cream, fontSize: 14, lineHeight: 20 },

  saveBtn: { backgroundColor: COLORS.accent, paddingVertical: 14, borderRadius: 8, alignItems: 'center', marginTop: 12 },
  saveBtnDisabled: { opacity: 0.6 },
  saveBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 13, letterSpacing: 1.2 },
});
