import { useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { COLORS, FONTS } from '../../constants/config';
import { trainerStudentsAPI, fileUrl } from '../../services/api';

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

export default function MyStudentDetailScreen({ navigation, route }) {
  const learnerId = route.params?.learnerId;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!learnerId) return;
    try {
      const res = await trainerStudentsAPI.getById(learnerId);
      setData(res.data || null);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, [learnerId]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  if (loading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.gold} />
        </View>
      </SafeAreaView>
    );
  }

  if (!data) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => navigation.goBack()}>
            <Text style={styles.backLinkText}>‹ Retour</Text>
          </TouchableOpacity>
          <Text style={styles.heading}>Apprenant</Text>
          <View style={{ width: 60 }} />
        </View>
        <View style={styles.center}>
          <Text style={styles.emptyText}>Apprenant introuvable.</Text>
        </View>
      </SafeAreaView>
    );
  }

  const photo = fileUrl(data.photoUrl);
  const initial = (data.firstName?.[0] ?? '') + (data.lastName?.[0] ?? '');
  const palette = STATUS_COLORS[data.status] || STATUS_COLORS.ACTIVE;
  const pct = Math.min(100, Math.max(0, Number(data.completionPercentage) || 0));

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading}>Apprenant</Text>
        <View style={{ width: 60 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.profileCard}>
          {photo ? (
            <Image source={{ uri: photo }} style={styles.avatarImg} />
          ) : (
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>{initial.toUpperCase()}</Text>
            </View>
          )}
          <Text style={styles.name}>
            {data.firstName} {data.lastName}
          </Text>
          <Text style={styles.email}>{data.email}</Text>
          <View
            style={[
              styles.badge,
              { backgroundColor: palette.bg, borderColor: palette.border, marginTop: 8 },
            ]}
          >
            <Text style={[styles.badgeText, { color: palette.text }]}>
              {STATUS_LABEL[data.status] || data.status}
            </Text>
          </View>
          <Text style={styles.subtitle}>Niveau {data.levelCode}</Text>

          <View style={styles.progressWrap}>
            <View style={styles.progressBar}>
              <View style={[styles.progressFill, { width: `${pct}%` }]} />
            </View>
            <Text style={styles.progressText}>{pct.toFixed(0)}% complété</Text>
          </View>

          <TouchableOpacity
            style={styles.messageBtn}
            onPress={() =>
              navigation.navigate('Chat', {
                recipientId: data.id,
                recipientName: `${data.firstName} ${data.lastName}`,
              })
            }
          >
            <Text style={styles.messageBtnText}>ENVOYER UN MESSAGE</Text>
          </TouchableOpacity>
        </View>

        <Text style={styles.sectionLabel}>STATISTIQUES</Text>
        <View style={styles.statsGrid}>
          <StatCard label="Cours complétés" value={String(data.coursesCompleted ?? 0)} />
          <StatCard label="QCM passés" value={String(data.qcmAttemptsCount ?? 0)} />
          <StatCard
            label="Score moyen"
            value={`${formatPct(data.qcmAvgScore)}`}
          />
          <StatCard label="Sessions suivies" value={String(data.sessionsAttended ?? 0)} />
          <StatCard label="Minutes" value={String(data.totalMinutes ?? 0)} />
          <StatCard label="Certifié" value={data.certified ? 'Oui' : 'Non'} />
        </View>

        <Text style={styles.sectionLabel}>MESSAGES RÉCENTS</Text>
        <View style={styles.messagesCard}>
          {!data.recentMessages || data.recentMessages.length === 0 ? (
            <Text style={styles.emptyText}>Aucun message échangé.</Text>
          ) : (
            data.recentMessages.map((m) => (
              <View key={m.id} style={styles.msgRow}>
                <Text style={styles.msgSender}>
                  {m.senderId === data.id ? `${data.firstName}` : 'Vous'}
                </Text>
                <Text style={styles.msgContent}>{m.content}</Text>
                <Text style={styles.msgDate}>{formatDate(m.sentAt)}</Text>
              </View>
            ))
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function StatCard({ label, value }) {
  return (
    <View style={styles.statCard}>
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

function formatDate(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    return `${d.toLocaleDateString()} ${d.toLocaleTimeString().slice(0, 5)}`;
  } catch {
    return iso;
  }
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
  scroll: { padding: 20, paddingBottom: 60 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  profileCard: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    padding: 20,
    alignItems: 'center',
    marginBottom: 24,
  },
  avatar: {
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: COLORS.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  avatarImg: {
    width: 84,
    height: 84,
    borderRadius: 42,
    marginBottom: 12,
  },
  avatarText: {
    fontFamily: FONTS.displayBold,
    color: COLORS.parchment,
    fontSize: 28,
  },
  name: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 20,
  },
  email: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 13,
    marginTop: 4,
  },
  subtitle: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 12,
    marginTop: 8,
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 4,
    borderWidth: 1,
  },
  badgeText: {
    fontFamily: FONTS.uiBold,
    fontSize: 10,
    letterSpacing: 0.8,
  },
  progressWrap: {
    width: '100%',
    marginTop: 16,
  },
  progressBar: {
    height: 8,
    backgroundColor: 'rgba(126,102,58,0.2)',
    borderRadius: 4,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: COLORS.gold,
  },
  progressText: {
    fontFamily: FONTS.ui,
    color: COLORS.cream,
    fontSize: 12,
    textAlign: 'center',
    marginTop: 6,
  },
  messageBtn: {
    backgroundColor: COLORS.accent,
    paddingHorizontal: 22,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 18,
  },
  messageBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 12,
    letterSpacing: 1.2,
  },

  sectionLabel: {
    fontFamily: FONTS.uiBold,
    color: COLORS.gold,
    fontSize: 10,
    letterSpacing: 2,
    marginBottom: 8,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 24,
  },
  statCard: {
    width: '47%',
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  statValue: {
    fontFamily: FONTS.displayBold,
    color: COLORS.parchment,
    fontSize: 20,
  },
  statLabel: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 11,
    marginTop: 2,
  },

  messagesCard: {
    backgroundColor: 'rgba(245,239,227,0.04)',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.15)',
    padding: 14,
    marginBottom: 20,
  },
  msgRow: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(126,102,58,0.12)',
  },
  msgSender: {
    fontFamily: FONTS.uiBold,
    color: COLORS.gold,
    fontSize: 11,
    letterSpacing: 0.5,
  },
  msgContent: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 13,
    marginTop: 2,
  },
  msgDate: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 10,
    marginTop: 2,
  },
  emptyText: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 13,
    textAlign: 'center',
  },
});
