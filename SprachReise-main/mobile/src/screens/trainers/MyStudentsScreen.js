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
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { COLORS, FONTS } from '../../constants/config';
import { trainerStudentsAPI, fileUrl } from '../../services/api';

const FILTERS = [
  { key: 'ALL', label: 'Tous' },
  { key: 'ACTIVE', label: 'Actifs' },
  { key: 'LATE', label: 'En retard' },
  { key: 'CERTIFIED', label: 'Certifiés' },
];

const STATUS_COLORS = {
  ACTIVE: { bg: 'rgba(16,185,129,0.15)', border: 'rgba(16,185,129,0.45)', text: COLORS.success },
  LATE: { bg: 'rgba(239,68,68,0.15)', border: 'rgba(239,68,68,0.45)', text: COLORS.error },
  CERTIFIED: { bg: 'rgba(184,137,58,0.2)', border: 'rgba(184,137,58,0.45)', text: COLORS.gold },
};

const STATUS_LABEL = {
  ACTIVE: 'Actif',
  LATE: 'En retard',
  CERTIFIED: 'Certifié',
};

export default function MyStudentsScreen({ navigation }) {
  const [students, setStudents] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState('ALL');

  const load = useCallback(async (currentFilter) => {
    try {
      const status = currentFilter && currentFilter !== 'ALL' ? currentFilter : undefined;
      const [studentsRes, statsRes] = await Promise.all([
        trainerStudentsAPI.list(status),
        trainerStudentsAPI.stats(),
      ]);
      setStudents(Array.isArray(studentsRes.data) ? studentsRes.data : []);
      setStats(statsRes.data || null);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Chargement impossible');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load(filter);
    }, [load, filter])
  );

  const onRefresh = () => {
    setRefreshing(true);
    load(filter);
  };

  const changeFilter = (key) => {
    setFilter(key);
    setLoading(true);
    load(key);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading}>Mes apprenants</Text>
        <View style={{ width: 60 }} />
      </View>

      {stats && (
        <View style={styles.statsRow}>
          <StatBox label="Apprenants" value={String(stats.totalStudents ?? 0)} />
          <StatBox label="Rétention" value={`${formatPct(stats.retentionRate)}%`} />
          <StatBox label="Score moy." value={`${formatPct(stats.avgScore)}`} />
          <StatBox label="Progrès moy." value={`${formatPct(stats.avgProgress)}%`} />
        </View>
      )}

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.chipsRow}
      >
        {FILTERS.map((f) => (
          <TouchableOpacity
            key={f.key}
            style={[styles.chip, filter === f.key && styles.chipActive]}
            onPress={() => changeFilter(f.key)}
          >
            <Text style={[styles.chipText, filter === f.key && styles.chipTextActive]}>
              {f.label}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.gold} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={styles.scroll}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={COLORS.gold} />
          }
        >
          {students.length === 0 ? (
            <View style={styles.empty}>
              <Text style={styles.emptyTitle}>Aucun apprenant</Text>
              <Text style={styles.emptyText}>
                Aucun apprenant ne correspond au filtre sélectionné.
              </Text>
            </View>
          ) : (
            students.map((s) => {
              const photo = fileUrl(s.photoUrl);
              const initial = (s.firstName?.[0] ?? '') + (s.lastName?.[0] ?? '');
              const palette = STATUS_COLORS[s.status] || STATUS_COLORS.ACTIVE;
              const pct = Math.min(100, Math.max(0, Number(s.completionPercentage) || 0));
              return (
                <TouchableOpacity
                  key={s.id}
                  style={styles.card}
                  onPress={() => navigation.navigate('MyStudentDetail', { learnerId: s.id })}
                >
                  <View style={styles.cardRow}>
                    {photo ? (
                      <Image source={{ uri: photo }} style={styles.avatarImg} />
                    ) : (
                      <View style={styles.avatar}>
                        <Text style={styles.avatarText}>{initial.toUpperCase()}</Text>
                      </View>
                    )}
                    <View style={styles.cardBody}>
                      <View style={styles.cardHead}>
                        <Text style={styles.cardName} numberOfLines={1}>
                          {s.firstName} {s.lastName}
                        </Text>
                        <View
                          style={[
                            styles.badge,
                            { backgroundColor: palette.bg, borderColor: palette.border },
                          ]}
                        >
                          <Text style={[styles.badgeText, { color: palette.text }]}>
                            {STATUS_LABEL[s.status] || s.status}
                          </Text>
                        </View>
                      </View>
                      <Text style={styles.cardMeta}>
                        Niveau {s.levelCode} · {pct.toFixed(0)}% complété
                      </Text>
                      <View style={styles.progressBar}>
                        <View style={[styles.progressFill, { width: `${pct}%` }]} />
                      </View>
                    </View>
                  </View>
                </TouchableOpacity>
              );
            })
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

function StatBox({ label, value }) {
  return (
    <View style={styles.statBox}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

function formatPct(v) {
  if (v === null || v === undefined) return '0';
  const n = Number(v);
  if (Number.isNaN(n)) return '0';
  return n.toFixed(0);
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 8,
    paddingBottom: 12,
    gap: 12,
  },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14, width: 60 },
  heading: {
    flex: 1,
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 22,
    textAlign: 'center',
  },

  statsRow: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 20,
    marginBottom: 12,
  },
  statBox: {
    flex: 1,
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 8,
    paddingVertical: 10,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  statValue: {
    fontFamily: FONTS.displayBold,
    color: COLORS.parchment,
    fontSize: 16,
  },
  statLabel: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 10,
    marginTop: 2,
  },

  chipsRow: {
    paddingHorizontal: 20,
    paddingBottom: 12,
    gap: 8,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.35)',
    backgroundColor: 'rgba(245,239,227,0.04)',
    marginRight: 8,
  },
  chipActive: {
    backgroundColor: COLORS.accent,
    borderColor: COLORS.accent,
  },
  chipText: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.cream,
    fontSize: 12,
  },
  chipTextActive: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
  },

  scroll: { padding: 20, paddingBottom: 60 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  empty: { alignItems: 'center', paddingVertical: 60 },
  emptyTitle: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 20,
    marginBottom: 8,
  },
  emptyText: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 14,
    textAlign: 'center',
    paddingHorizontal: 30,
  },

  card: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    marginBottom: 12,
    padding: 12,
  },
  cardRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: COLORS.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarImg: {
    width: 48,
    height: 48,
    borderRadius: 24,
  },
  avatarText: {
    fontFamily: FONTS.displayBold,
    color: COLORS.parchment,
    fontSize: 16,
  },
  cardBody: { flex: 1 },
  cardHead: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 4,
  },
  cardName: {
    flex: 1,
    fontFamily: FONTS.displayBold,
    color: COLORS.parchment,
    fontSize: 15,
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
    borderWidth: 1,
  },
  badgeText: {
    fontFamily: FONTS.uiBold,
    fontSize: 9,
    letterSpacing: 0.8,
  },
  cardMeta: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 12,
    marginBottom: 6,
  },
  progressBar: {
    height: 6,
    backgroundColor: 'rgba(126,102,58,0.2)',
    borderRadius: 3,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: COLORS.gold,
  },
});
