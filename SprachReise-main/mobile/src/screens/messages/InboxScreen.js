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
import { messagesAPI, fileUrl } from '../../services/api';

export default function InboxScreen({ navigation }) {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const res = await messagesAPI.inbox();
      setConversations(Array.isArray(res.data) ? res.data : []);
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

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading}>Messages</Text>
        <View style={{ width: 60 }} />
      </View>

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
          {conversations.length === 0 ? (
            <View style={styles.empty}>
              <Text style={styles.emptyTitle}>Aucun message</Text>
              <Text style={styles.emptyText}>
                Vos conversations apparaîtront ici.
              </Text>
            </View>
          ) : (
            conversations.map((c) => {
              const photo = fileUrl(c.otherPhotoUrl);
              const initial =
                (c.otherFirstName?.[0] ?? '') + (c.otherLastName?.[0] ?? '');
              const unread = Number(c.unreadCount) || 0;
              return (
                <TouchableOpacity
                  key={c.otherUserId}
                  style={styles.card}
                  onPress={() =>
                    navigation.navigate('Chat', {
                      recipientId: c.otherUserId,
                      recipientName: `${c.otherFirstName} ${c.otherLastName}`,
                    })
                  }
                >
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
                        {c.otherFirstName} {c.otherLastName}
                      </Text>
                      <Text style={styles.cardDate}>{formatDate(c.lastSentAt)}</Text>
                    </View>
                    <View style={styles.cardFoot}>
                      <Text
                        style={[styles.cardPreview, unread > 0 && styles.cardPreviewUnread]}
                        numberOfLines={1}
                      >
                        {c.lastMessage}
                      </Text>
                      {unread > 0 && (
                        <View style={styles.unreadBadge}>
                          <Text style={styles.unreadBadgeText}>{unread}</Text>
                        </View>
                      )}
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

function formatDate(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) {
      return d.toLocaleTimeString().slice(0, 5);
    }
    return d.toLocaleDateString();
  } catch {
    return '';
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
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    padding: 12,
    marginBottom: 10,
  },
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
    justifyContent: 'space-between',
    marginBottom: 4,
    gap: 8,
  },
  cardName: {
    flex: 1,
    fontFamily: FONTS.displayBold,
    color: COLORS.parchment,
    fontSize: 15,
  },
  cardDate: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 11,
  },
  cardFoot: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  cardPreview: {
    flex: 1,
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 13,
  },
  cardPreviewUnread: {
    fontFamily: FONTS.medium,
    color: COLORS.cream,
  },
  unreadBadge: {
    backgroundColor: COLORS.gold,
    borderRadius: 10,
    minWidth: 20,
    height: 20,
    paddingHorizontal: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  unreadBadgeText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.deep,
    fontSize: 11,
  },
});
