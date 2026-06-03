import { useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Linking,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { COLORS, FONTS } from '../../constants/config';
import { qcmsAPI } from '../../services/api';

export default function QcmResultsScreen({ navigation, route }) {
  const qcm = route?.params?.qcm;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!qcm?.id) return;
    setLoading(true);
    try {
      const res = await qcmsAPI.getResults(qcm.id);
      setData(res.data);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message);
    } finally {
      setLoading(false);
    }
  }, [qcm?.id]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const openPdf = async () => {
    if (!qcm?.id) return;
    const url = qcmsAPI.resultsPdfUrl(qcm.id);
    try {
      const can = await Linking.canOpenURL(url);
      if (!can) throw new Error('URL non ouvrable');
      await Linking.openURL(url);
    } catch (e) {
      Alert.alert('Erreur', e.message || 'Impossible d\'ouvrir le PDF');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading} numberOfLines={1}>{qcm?.title || 'Résultats'}</Text>
        <View style={{ width: 60 }} />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.gold} />
        </View>
      ) : (
        <ScrollView contentContainerStyle={styles.scroll}>
          <View style={styles.summary}>
            <Text style={styles.summaryLabel}>TENTATIVES</Text>
            <Text style={styles.summaryValue}>{data?.total ?? 0}</Text>
            <Text style={styles.summaryHint}>
              Niveau {data?.levelCode || qcm?.levelCode || '?'}
            </Text>
          </View>

          <TouchableOpacity style={styles.pdfBtn} onPress={openPdf}>
            <Text style={styles.pdfBtnText}>📄 TÉLÉCHARGER PDF</Text>
          </TouchableOpacity>

          <Text style={styles.sectionTitle}>Tentatives</Text>
          {(data?.attempts || []).length === 0 ? (
            <Text style={styles.emptyText}>Aucune tentative pour ce QCM.</Text>
          ) : (
            (data?.attempts || []).map((a) => (
              <View key={a.id} style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.rowName}>{a.learnerName}</Text>
                  <Text style={styles.rowMeta}>
                    {a.attemptedAt || '-'} · {a.correctAnswers ?? 0}/{a.totalQuestions ?? 0} bonnes
                  </Text>
                </View>
                <Text style={styles.rowScore}>{a.score != null ? `${a.score}` : '-'}</Text>
              </View>
            ))
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

  summary: {
    backgroundColor: 'rgba(184,137,58,0.12)',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.35)',
    padding: 16,
    marginBottom: 16,
    alignItems: 'center',
  },
  summaryLabel: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 10, letterSpacing: 1.4 },
  summaryValue: { fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 32, marginVertical: 4 },
  summaryHint: { fontFamily: FONTS.ui, color: COLORS.cream, fontSize: 12 },

  pdfBtn: {
    backgroundColor: COLORS.accent,
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 20,
  },
  pdfBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 12, letterSpacing: 1.2 },

  sectionTitle: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 10, letterSpacing: 2, marginBottom: 10 },
  emptyText: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 14, textAlign: 'center', marginTop: 24 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(245,239,227,0.05)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginBottom: 8,
    gap: 12,
  },
  rowName: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 14 },
  rowMeta: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 11, marginTop: 2 },
  rowScore: { fontFamily: FONTS.displayBold, color: COLORS.gold, fontSize: 18 },
});
