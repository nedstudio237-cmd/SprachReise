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
import { coursesAPI } from '../../services/api';

const STATUS_COLORS = {
  DRAFT: { bg: 'rgba(245,158,11,0.15)', border: 'rgba(245,158,11,0.45)', text: COLORS.warning },
  PUBLISHED: { bg: 'rgba(16,185,129,0.15)', border: 'rgba(16,185,129,0.45)', text: COLORS.success },
  REMOVED: { bg: 'rgba(239,68,68,0.15)', border: 'rgba(239,68,68,0.45)', text: COLORS.error },
};

export default function MyCoursesScreen({ navigation }) {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const res = await coursesAPI.getMine();
      setCourses(Array.isArray(res.data) ? res.data : []);
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

  const removeOne = (course) => {
    Alert.alert(
      'Supprimer ce cours ?',
      `« ${course.title} » sera marqué comme supprimé.`,
      [
        { text: 'Annuler', style: 'cancel' },
        {
          text: 'Supprimer',
          style: 'destructive',
          onPress: async () => {
            try {
              await coursesAPI.remove(course.id);
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
        <Text style={styles.heading}>Mes cours</Text>
        <TouchableOpacity
          style={styles.newBtn}
          onPress={() => navigation.navigate('CreateCourse')}
        >
          <Text style={styles.newBtnText}>+ NOUVEAU</Text>
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
          {courses.length === 0 ? (
            <View style={styles.empty}>
              <Text style={styles.emptyTitle}>Aucun cours</Text>
              <Text style={styles.emptyText}>
                Crée ton premier cours pour le rendre disponible aux apprenants.
              </Text>
              <TouchableOpacity
                style={styles.emptyBtn}
                onPress={() => navigation.navigate('CreateCourse')}
              >
                <Text style={styles.emptyBtnText}>CRÉER UN COURS</Text>
              </TouchableOpacity>
            </View>
          ) : (
            courses.map((c) => {
              const palette = STATUS_COLORS[c.status] || STATUS_COLORS.DRAFT;
              return (
                <View key={c.id} style={styles.card}>
                  <TouchableOpacity
                    style={styles.cardMain}
                    onPress={() => navigation.navigate('CreateCourse', { course: c })}
                  >
                    <View style={styles.cardHead}>
                      <Text style={styles.cardTitle} numberOfLines={1}>{c.title}</Text>
                      <View style={[styles.badge, { backgroundColor: palette.bg, borderColor: palette.border }]}>
                        <Text style={[styles.badgeText, { color: palette.text }]}>{c.status}</Text>
                      </View>
                    </View>
                    <Text style={styles.cardMeta}>
                      {c.levelCode || '?'} · {c.theme || 'Sans thème'} · {c.viewCount ?? 0} vues
                    </Text>
                    {!!c.description && (
                      <Text style={styles.cardDesc} numberOfLines={2}>{c.description}</Text>
                    )}
                  </TouchableOpacity>
                  {c.status !== 'REMOVED' && (
                    <TouchableOpacity style={styles.deleteBtn} onPress={() => removeOne(c)}>
                      <Text style={styles.deleteBtnText}>Supprimer</Text>
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
    fontSize: 22,
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
  },
  deleteBtn: {
    paddingVertical: 10,
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: 'rgba(126,102,58,0.2)',
    backgroundColor: 'rgba(239,68,68,0.06)',
  },
  deleteBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.error,
    fontSize: 11,
    letterSpacing: 1,
  },
});
