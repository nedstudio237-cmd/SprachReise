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
import { sessionsAPI } from '../../services/api';

const STATUS_COLORS = {
  SCHEDULED: { bg: 'rgba(245,158,11,0.15)', border: 'rgba(245,158,11,0.45)', text: COLORS.warning },
  LIVE:      { bg: 'rgba(239,68,68,0.20)',  border: 'rgba(239,68,68,0.55)',  text: COLORS.error },
  ENDED:     { bg: 'rgba(16,185,129,0.15)', border: 'rgba(16,185,129,0.45)', text: COLORS.success },
  CANCELLED: { bg: 'rgba(174,145,130,0.15)', border: 'rgba(174,145,130,0.35)', text: COLORS.muted },
};

function formatStart(iso) {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    return d.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

function canStart(session) {
  if (session.status !== 'SCHEDULED' || !session.scheduledStart) return false;
  const start = new Date(session.scheduledStart).getTime();
  if (isNaN(start)) return false;
  const now = Date.now();
  // within 5 min before start (or already past)
  return start - now <= 5 * 60 * 1000;
}

export default function MySessionsScreen({ navigation }) {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const res = await sessionsAPI.getMine();
      setSessions(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Chargement impossible');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  const onRefresh = () => {
    setRefreshing(true);
    load();
  };

  const startSession = async (s) => {
    try {
      const res = await sessionsAPI.start(s.id);
      navigation.navigate('LiveSession', {
        sessionId: s.id,
        title: s.title,
        agoraChannel: res.data?.agoraChannel,
        token: res.data?.token,
        role: 'HOST',
      });
      load();
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Démarrage impossible');
    }
  };

  const endSession = (s) => {
    Alert.alert(
      'Terminer la session ?',
      `« ${s.title} » sera marquée comme terminée.`,
      [
        { text: 'Annuler', style: 'cancel' },
        {
          text: 'Terminer',
          style: 'destructive',
          onPress: async () => {
            try {
              await sessionsAPI.end(s.id);
              load();
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
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading}>Mes sessions live</Text>
        <TouchableOpacity
          style={styles.newBtn}
          onPress={() => navigation.navigate('CreateSession')}
        >
          <Text style={styles.newBtnText}>+ NOUVELLE</Text>
        </TouchableOpacity>
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
          {sessions.length === 0 ? (
            <View style={styles.empty}>
              <Text style={styles.emptyTitle}>Aucune session</Text>
              <Text style={styles.emptyText}>
                Planifie ta première session live pour tes apprenants.
              </Text>
              <TouchableOpacity
                style={styles.emptyBtn}
                onPress={() => navigation.navigate('CreateSession')}
              >
                <Text style={styles.emptyBtnText}>PROGRAMMER UNE SESSION</Text>
              </TouchableOpacity>
            </View>
          ) : (
            sessions.map((s) => {
              const palette = STATUS_COLORS[s.status] || STATUS_COLORS.SCHEDULED;
              const startable = canStart(s);
              return (
                <View key={s.id} style={styles.card}>
                  <TouchableOpacity
                    style={styles.cardMain}
                    onPress={() => navigation.navigate('CreateSession', { session: s })}
                  >
                    <View style={styles.cardHead}>
                      <Text style={styles.cardTitle} numberOfLines={1}>{s.title}</Text>
                      <View style={[styles.badge, { backgroundColor: palette.bg, borderColor: palette.border }]}>
                        <Text style={[styles.badgeText, { color: palette.text }]}>{s.status}</Text>
                      </View>
                    </View>
                    <Text style={styles.cardMeta}>
                      {formatStart(s.scheduledStart)} · {s.durationMinutes ?? 60} min
                    </Text>
                    {!!s.description && (
                      <Text style={styles.cardDesc} numberOfLines={2}>{s.description}</Text>
                    )}
                    <Text style={styles.cardChannel}>Canal : {s.agoraChannel}</Text>
                  </TouchableOpacity>

                  {s.status === 'SCHEDULED' && startable && (
                    <TouchableOpacity style={styles.startBtn} onPress={() => startSession(s)}>
                      <Text style={styles.startBtnText}>▶ DÉMARRER</Text>
                    </TouchableOpacity>
                  )}
                  {s.status === 'LIVE' && (
                    <TouchableOpacity style={styles.endBtn} onPress={() => endSession(s)}>
                      <Text style={styles.endBtnText}>■ TERMINER</Text>
                    </TouchableOpacity>
                  )}
                </View>
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
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 8,
    paddingBottom: 12,
    gap: 12,
  },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },
  heading: {
    flex: 1,
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 20,
    textAlign: 'center',
  },
  newBtn: {
    backgroundColor: COLORS.accent,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 6,
  },
  newBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 11,
    letterSpacing: 1,
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
    marginBottom: 20,
    paddingHorizontal: 30,
  },
  emptyBtn: {
    backgroundColor: COLORS.accent,
    paddingHorizontal: 22,
    paddingVertical: 12,
    borderRadius: 8,
  },
  emptyBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 12,
    letterSpacing: 1.2,
  },

  card: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    marginBottom: 12,
    overflow: 'hidden',
  },
  cardMain: { padding: 14 },
  cardHead: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 6,
  },
  cardTitle: {
    flex: 1,
    fontFamily: FONTS.displayBold,
    color: COLORS.parchment,
    fontSize: 16,
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 4,
    borderWidth: 1,
  },
  badgeText: {
    fontFamily: FONTS.uiBold,
    fontSize: 10,
    letterSpacing: 1,
  },
  cardMeta: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 12,
    marginBottom: 4,
  },
  cardDesc: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 13,
    marginBottom: 6,
  },
  cardChannel: {
    fontFamily: FONTS.ui,
    color: 'rgba(174,145,130,0.6)',
    fontSize: 11,
  },
  startBtn: {
    paddingVertical: 12,
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: 'rgba(126,102,58,0.2)',
    backgroundColor: 'rgba(16,185,129,0.12)',
  },
  startBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.success,
    fontSize: 12,
    letterSpacing: 1,
  },
  endBtn: {
    paddingVertical: 12,
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: 'rgba(126,102,58,0.2)',
    backgroundColor: 'rgba(239,68,68,0.10)',
  },
  endBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.error,
    fontSize: 12,
    letterSpacing: 1,
  },
});
