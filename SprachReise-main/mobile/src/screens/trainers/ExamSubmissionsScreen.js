import { useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  RefreshControl,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { COLORS, FONTS } from '../../constants/config';
import { examsAPI } from '../../services/api';

export default function ExamSubmissionsScreen({ navigation, route }) {
  const exam = route?.params?.exam;
  const [subs, setSubs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    if (!exam?.id) return;
    try {
      const res = await examsAPI.listSubmissions(exam.id);
      setSubs(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [exam?.id]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const onRefresh = () => { setRefreshing(true); load(); };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading} numberOfLines={1}>{exam?.title || 'Copies'}</Text>
        <View style={{ width: 60 }} />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.gold} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={styles.scroll}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={COLORS.gold} />}
        >
          {subs.length === 0 ? (
            <Text style={styles.emptyText}>Aucune copie pour le moment.</Text>
          ) : (
            subs.map((s) => {
              const graded = s.grade != null;
              return (
                <TouchableOpacity
                  key={s.id}
                  style={styles.card}
                  onPress={() => navigation.navigate('GradeSubmission', { submission: s, exam })}
                >
                  <View style={styles.cardHead}>
                    <Text style={styles.cardName} numberOfLines={1}>{s.learnerName}</Text>
                    <View style={[styles.badge, graded ? styles.badgeOk : styles.badgeWait]}>
                      <Text style={[styles.badgeText, graded ? { color: COLORS.success } : { color: COLORS.warning }]}>
                        {graded ? `${s.grade}/20` : 'À corriger'}
                      </Text>
                    </View>
                  </View>
                  <Text style={styles.cardMeta}>
                    Soumis le {s.submittedAt ? String(s.submittedAt).replace('T', ' ').slice(0, 16) : '-'}
                  </Text>
                  {s.answerText ? (
                    <Text style={styles.cardPreview} numberOfLines={3}>{s.answerText}</Text>
                  ) : null}
                </TouchableOpacity>
              );
            })
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 20, paddingTop: 8, paddingBottom: 12, gap: 12,
  },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14, width: 60 },
  heading: { flex: 1, fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 20, textAlign: 'center' },

  scroll: { padding: 20, paddingBottom: 60 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  emptyText: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 14, textAlign: 'center', marginTop: 40 },

  card: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    padding: 14,
    marginBottom: 12,
  },
  cardHead: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 6 },
  cardName: { flex: 1, fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 15 },
  badge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 4, borderWidth: 1 },
  badgeOk: { backgroundColor: 'rgba(16,185,129,0.15)', borderColor: 'rgba(16,185,129,0.45)' },
  badgeWait: { backgroundColor: 'rgba(245,158,11,0.15)', borderColor: 'rgba(245,158,11,0.45)' },
  badgeText: { fontFamily: FONTS.uiBold, fontSize: 10, letterSpacing: 0.8 },
  cardMeta: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 11, marginBottom: 6 },
  cardPreview: { fontFamily: FONTS.regular, color: COLORS.cream, fontSize: 13 },
});
