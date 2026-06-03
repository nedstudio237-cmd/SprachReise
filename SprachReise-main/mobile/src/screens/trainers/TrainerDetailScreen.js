import { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { COLORS, FONTS } from '../../constants/config';
import { trainersAPI, fileUrl } from '../../services/api';

const LEVEL_COLORS = {
  A1: '#10B981',
  A2: '#3B82F6',
  B1: '#8B5CF6',
  B2: '#F59E0B',
  C1: '#EC4899',
  C2: '#EF4444',
};

function formatDateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function TrainerDetailScreen({ route, navigation }) {
  const userId = route?.params?.userId;
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await trainersAPI.getById(userId);
        if (!cancelled) setDetail(res.data);
      } catch (e) {
        if (!cancelled) setError(e.message || 'Erreur de chargement');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [userId]);

  if (loading) {
    return (
      <SafeAreaView style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color={COLORS.gold} />
      </SafeAreaView>
    );
  }

  if (error || !detail) {
    return (
      <SafeAreaView style={[styles.container, styles.center]}>
        <Text style={styles.errorText}>⚠️ {error || 'Formateur introuvable'}</Text>
        <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
          <Text style={styles.backBtnText}>RETOUR</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  const { profile, publishedCourses, upcomingSessions } = detail;
  const accent = LEVEL_COLORS[profile.assignedLevelCode] || COLORS.gold;
  const initials = `${profile.firstName?.[0] || ''}${profile.lastName?.[0] || ''}`.toUpperCase();
  const full = (profile.maxStudents || 0) - (profile.placesLeft || 0);
  const pct = profile.maxStudents ? Math.min(100, Math.round((full / profile.maxStudents) * 100)) : 0;
  const photo = fileUrl(profile.photoUrl);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <TouchableOpacity style={styles.backLink} onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>

        <View style={styles.header}>
          {photo ? (
            <Image source={{ uri: photo }} style={[styles.avatar, { borderColor: accent }]} />
          ) : (
            <View style={[styles.avatar, { backgroundColor: accent + '22', borderColor: accent, alignItems: 'center', justifyContent: 'center' }]}>
              <Text style={[styles.avatarText, { color: accent }]}>{initials}</Text>
            </View>
          )}
          <Text style={styles.name}>{profile.firstName} {profile.lastName}</Text>
          {profile.city ? <Text style={styles.city}>📍 {profile.city}</Text> : null}
          <View style={styles.badgeRow}>
            <View style={[styles.levelBadge, { backgroundColor: accent + '22' }]}>
              <Text style={[styles.levelBadgeText, { color: accent }]}>Niveau {profile.assignedLevelCode}</Text>
            </View>
            <Text style={styles.rating}>⭐ {Number(profile.ratingAvg).toFixed(2)}</Text>
          </View>
        </View>

        {profile.bio ? (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>BIOGRAPHIE</Text>
            <Text style={styles.bio}>{profile.bio}</Text>
          </View>
        ) : null}

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>DISPONIBILITÉ</Text>
          <View style={styles.quotaCard}>
            <View style={styles.quotaTop}>
              <Text style={styles.quotaLabel}>Apprenants inscrits</Text>
              <Text style={styles.quotaCount}>{full} / {profile.maxStudents}</Text>
            </View>
            <View style={styles.quotaBar}>
              <View style={[styles.quotaFill, { width: `${pct}%`, backgroundColor: accent }]} />
            </View>
            <Text style={styles.quotaSub}>
              {profile.placesLeft > 0
                ? `${profile.placesLeft} place${profile.placesLeft > 1 ? 's' : ''} restante${profile.placesLeft > 1 ? 's' : ''}`
                : 'Complet'}
            </Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>COURS PUBLIÉS ({publishedCourses?.length || 0})</Text>
          {(publishedCourses || []).length === 0 ? (
            <Text style={styles.empty}>Aucun cours publié pour le moment.</Text>
          ) : (
            publishedCourses.map((c) => (
              <TouchableOpacity
                key={c.id}
                style={styles.itemCard}
                onPress={() => navigation.navigate('CourseDetail', { course: c })}
                activeOpacity={0.82}
              >
                <View style={[styles.itemAccent, { backgroundColor: accent }]} />
                <View style={{ flex: 1 }}>
                  <Text style={styles.itemTitle}>{c.title}</Text>
                  <Text style={styles.itemSub}>
                    {c.theme || 'Cours'} · {c.videoDurationSec ? `${Math.round(c.videoDurationSec / 60)} min` : ''}
                  </Text>
                </View>
                <Text style={styles.itemArrow}>›</Text>
              </TouchableOpacity>
            ))
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>SESSIONS À VENIR ({upcomingSessions?.length || 0})</Text>
          {(upcomingSessions || []).length === 0 ? (
            <Text style={styles.empty}>Aucune session programmée.</Text>
          ) : (
            upcomingSessions.map((s) => (
              <View key={s.id} style={styles.sessionCard}>
                <View style={styles.sessionTop}>
                  <View style={styles.livePill}>
                    <View style={styles.liveDot} />
                    <Text style={styles.livePillText}>LIVE</Text>
                  </View>
                  <Text style={styles.sessionTime}>{formatDateTime(s.scheduledStart)}</Text>
                </View>
                <Text style={styles.sessionTitle}>{s.title}</Text>
                {s.description ? <Text style={styles.sessionDesc} numberOfLines={2}>{s.description}</Text> : null}
                <Text style={styles.sessionDuration}>{s.durationMinutes} min</Text>
              </View>
            ))
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },
  scroll: { padding: 20, paddingBottom: 40 },

  center: { alignItems: 'center', justifyContent: 'center' },
  errorText: { fontFamily: FONTS.regular, color: COLORS.error, marginBottom: 16 },
  backBtn: { backgroundColor: COLORS.accent, paddingHorizontal: 20, paddingVertical: 10, borderRadius: 5 },
  backBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 11, letterSpacing: 1 },

  backLink: { marginBottom: 8 },
  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14 },

  header: { alignItems: 'center', paddingVertical: 16 },
  avatar: {
    width: 88,
    height: 88,
    borderRadius: 44,
    borderWidth: 2,
    marginBottom: 12,
  },
  avatarText: { fontFamily: FONTS.uiBold, fontSize: 30 },
  name: { fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 22, marginBottom: 4 },
  city: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 13, marginBottom: 8 },
  badgeRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  levelBadge: { borderRadius: 5, paddingHorizontal: 10, paddingVertical: 4 },
  levelBadgeText: { fontFamily: FONTS.uiBold, fontSize: 11, letterSpacing: 0.5 },
  rating: { fontFamily: FONTS.uiMedium, color: COLORS.cream, fontSize: 13 },

  section: { marginTop: 24 },
  sectionTitle: {
    fontFamily: FONTS.uiBold,
    color: COLORS.muted,
    fontSize: 11,
    letterSpacing: 1.2,
    marginBottom: 12,
  },
  bio: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 14,
    lineHeight: 22,
  },

  quotaCard: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  quotaTop: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 10 },
  quotaLabel: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 13 },
  quotaCount: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 14 },
  quotaBar: {
    height: 8,
    backgroundColor: 'rgba(245,239,227,0.08)',
    borderRadius: 4,
    overflow: 'hidden',
  },
  quotaFill: { height: '100%', borderRadius: 4 },
  quotaSub: { fontFamily: FONTS.regular, color: COLORS.cream, fontSize: 12, marginTop: 8 },

  itemCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
    gap: 12,
  },
  itemAccent: { width: 4, height: 36, borderRadius: 2 },
  itemTitle: { fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 14 },
  itemSub: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 12, marginTop: 2 },
  itemArrow: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 24 },

  sessionCard: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  sessionTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  livePill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(239,68,68,0.15)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 4,
  },
  liveDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: COLORS.error },
  livePillText: { fontFamily: FONTS.uiBold, color: COLORS.error, fontSize: 9, letterSpacing: 0.8 },
  sessionTime: { fontFamily: FONTS.uiMedium, color: COLORS.cream, fontSize: 12 },
  sessionTitle: { fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 14, marginBottom: 4 },
  sessionDesc: { fontFamily: FONTS.regular, color: COLORS.cream, fontSize: 12, lineHeight: 18, marginBottom: 4 },
  sessionDuration: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 11 },

  empty: { fontFamily: FONTS.regular, color: COLORS.muted, fontStyle: 'italic', fontSize: 13 },
});
