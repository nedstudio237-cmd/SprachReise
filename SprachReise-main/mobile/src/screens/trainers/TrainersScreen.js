import { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
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

export default function TrainersScreen({ navigation }) {
  const [trainers, setTrainers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    try {
      setError(null);
      const res = await trainersAPI.list();
      setTrainers(res.data);
    } catch (e) {
      setError(e.message || 'Erreur de chargement');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    load();
  };

  const renderItem = ({ item }) => {
    const accent = LEVEL_COLORS[item.assignedLevelCode] || COLORS.gold;
    const initials = `${item.firstName?.[0] || ''}${item.lastName?.[0] || ''}`.toUpperCase();
    const full = ((item.maxStudents || 0) - (item.placesLeft || 0));
    const pct = item.maxStudents ? Math.min(100, Math.round((full / item.maxStudents) * 100)) : 0;
    const photo = fileUrl(item.photoUrl);

    return (
      <TouchableOpacity
        style={styles.card}
        onPress={() => navigation.navigate('TrainerDetail', { userId: item.userId, name: `${item.firstName} ${item.lastName}` })}
        activeOpacity={0.82}
      >
        <View style={styles.cardTop}>
          {photo ? (
            <Image source={{ uri: photo }} style={[styles.avatar, { borderColor: accent }]} />
          ) : (
            <View style={[styles.avatar, { backgroundColor: accent + '22', borderColor: accent, alignItems: 'center', justifyContent: 'center' }]}>
              <Text style={[styles.avatarText, { color: accent }]}>{initials}</Text>
            </View>
          )}
          <View style={{ flex: 1 }}>
            <Text style={styles.name}>{item.firstName} {item.lastName}</Text>
            {item.city ? <Text style={styles.city}>📍 {item.city}</Text> : null}
            <View style={styles.badgeRow}>
              <View style={[styles.levelBadge, { backgroundColor: accent + '22' }]}>
                <Text style={[styles.levelBadgeText, { color: accent }]}>{item.assignedLevelCode}</Text>
              </View>
              <Text style={styles.rating}>⭐ {Number(item.ratingAvg).toFixed(2)}</Text>
            </View>
          </View>
        </View>

        {item.bio ? <Text style={styles.bio} numberOfLines={2}>{item.bio}</Text> : null}

        <View style={styles.quotaRow}>
          <View style={styles.quotaBar}>
            <View style={[styles.quotaFill, { width: `${pct}%`, backgroundColor: accent }]} />
          </View>
          <Text style={styles.quotaText}>{item.placesLeft}/{item.maxStudents} places</Text>
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.topBar}>
        <Text style={styles.heading}>Formateurs</Text>
        <Text style={styles.sub}>Nos experts certifiés</Text>
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.gold} />
        </View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.errorText}>⚠️ {error}</Text>
          <TouchableOpacity style={styles.retryBtn} onPress={load}>
            <Text style={styles.retryBtnText}>RÉESSAYER</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <FlatList
          data={trainers}
          keyExtractor={(item) => String(item.userId)}
          renderItem={renderItem}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={COLORS.gold} />}
          ListEmptyComponent={<Text style={styles.empty}>Aucun formateur disponible.</Text>}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },

  topBar: { paddingHorizontal: 20, paddingTop: 24, paddingBottom: 8 },
  heading: { fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 28, marginBottom: 4 },
  sub: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 14, fontStyle: 'italic' },

  list: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 32 },

  card: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  cardTop: { flexDirection: 'row', alignItems: 'center', gap: 14 },

  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    borderWidth: 2,
  },
  avatarText: { fontFamily: FONTS.uiBold, fontSize: 18 },

  name: { fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 17 },
  city: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 12, marginTop: 2 },

  badgeRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 6 },
  levelBadge: { borderRadius: 5, paddingHorizontal: 9, paddingVertical: 3 },
  levelBadgeText: { fontFamily: FONTS.uiBold, fontSize: 11, letterSpacing: 0.5 },
  rating: { fontFamily: FONTS.uiMedium, color: COLORS.cream, fontSize: 12 },

  bio: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 13,
    lineHeight: 19,
    marginTop: 12,
  },

  quotaRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 12 },
  quotaBar: {
    flex: 1,
    height: 6,
    backgroundColor: 'rgba(245,239,227,0.08)',
    borderRadius: 3,
    overflow: 'hidden',
  },
  quotaFill: { height: '100%', borderRadius: 3 },
  quotaText: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 11 },

  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  errorText: { fontFamily: FONTS.regular, color: COLORS.error, fontSize: 14, textAlign: 'center', marginBottom: 16 },
  retryBtn: { backgroundColor: COLORS.accent, paddingHorizontal: 20, paddingVertical: 10, borderRadius: 5 },
  retryBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 11, letterSpacing: 1 },
  empty: { fontFamily: FONTS.regular, color: COLORS.muted, textAlign: 'center', marginTop: 40, fontSize: 14 },
});
