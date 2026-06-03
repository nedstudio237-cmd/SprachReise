import { useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  TextInput,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { COLORS, FONTS } from '../../constants/config';
import { messagesAPI } from '../../services/api';
import { useAuthStore } from '../../store/authStore';

const POLL_INTERVAL_MS = 5000;

export default function ChatScreen({ navigation, route }) {
  const recipientId = route.params?.recipientId;
  const recipientName = route.params?.recipientName || 'Conversation';
  const { user } = useAuthStore();
  const myId = user?.id;

  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [input, setInput] = useState('');
  const scrollRef = useRef(null);
  const pollRef = useRef(null);

  const load = useCallback(async () => {
    if (!recipientId) return;
    try {
      const res = await messagesAPI.conversation(recipientId);
      setMessages(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      if (loading) {
        Alert.alert('Erreur', e.response?.data?.error || e.message || 'Chargement impossible');
      }
    } finally {
      setLoading(false);
    }
  }, [recipientId, loading]);

  useEffect(() => {
    load();
    pollRef.current = setInterval(load, POLL_INTERVAL_MS);
    return () => {
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recipientId]);

  useEffect(() => {
    if (scrollRef.current) {
      setTimeout(() => scrollRef.current?.scrollToEnd({ animated: true }), 100);
    }
  }, [messages.length]);

  const send = async () => {
    const content = input.trim();
    if (!content || !recipientId || sending) return;
    setSending(true);
    try {
      const res = await messagesAPI.send(recipientId, content);
      setInput('');
      setMessages((prev) => [...prev, res.data]);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Envoi impossible');
    } finally {
      setSending(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading} numberOfLines={1}>
          {recipientName}
        </Text>
        <View style={{ width: 60 }} />
      </View>

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 80 : 0}
      >
        {loading ? (
          <View style={styles.center}>
            <ActivityIndicator size="large" color={COLORS.gold} />
          </View>
        ) : (
          <ScrollView
            ref={scrollRef}
            contentContainerStyle={styles.scroll}
            onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: false })}
          >
            {messages.length === 0 ? (
              <View style={styles.empty}>
                <Text style={styles.emptyText}>Aucun message. Démarrez la conversation.</Text>
              </View>
            ) : (
              messages.map((m) => {
                const mine = m.senderId === myId;
                return (
                  <View
                    key={m.id}
                    style={[styles.bubbleRow, mine ? styles.rowMine : styles.rowOther]}
                  >
                    <View style={[styles.bubble, mine ? styles.bubbleMine : styles.bubbleOther]}>
                      <Text style={[styles.bubbleText, mine && styles.bubbleTextMine]}>
                        {m.content}
                      </Text>
                      <Text style={[styles.bubbleDate, mine && styles.bubbleDateMine]}>
                        {formatTime(m.sentAt)}
                      </Text>
                    </View>
                  </View>
                );
              })
            )}
          </ScrollView>
        )}

        <View style={styles.inputBar}>
          <TextInput
            style={styles.input}
            placeholder="Écrire un message…"
            placeholderTextColor={COLORS.muted}
            value={input}
            onChangeText={setInput}
            multiline
            editable={!sending}
          />
          <TouchableOpacity
            style={[styles.sendBtn, (!input.trim() || sending) && styles.sendBtnDisabled]}
            onPress={send}
            disabled={!input.trim() || sending}
          >
            <Text style={styles.sendBtnText}>{sending ? '…' : 'Envoyer'}</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function formatTime(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    return d.toLocaleTimeString().slice(0, 5);
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
    fontSize: 18,
    textAlign: 'center',
  },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  scroll: { padding: 14, paddingBottom: 20 },

  empty: { alignItems: 'center', paddingVertical: 40 },
  emptyText: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    fontSize: 13,
    textAlign: 'center',
  },

  bubbleRow: { flexDirection: 'row', marginBottom: 8 },
  rowMine: { justifyContent: 'flex-end' },
  rowOther: { justifyContent: 'flex-start' },
  bubble: {
    maxWidth: '78%',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 12,
  },
  bubbleMine: {
    backgroundColor: COLORS.accent,
    borderBottomRightRadius: 2,
  },
  bubbleOther: {
    backgroundColor: 'rgba(245,239,227,0.1)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    borderBottomLeftRadius: 2,
  },
  bubbleText: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 14,
  },
  bubbleTextMine: {
    color: COLORS.parchment,
  },
  bubbleDate: {
    fontFamily: FONTS.ui,
    color: COLORS.muted,
    fontSize: 10,
    marginTop: 4,
    textAlign: 'right',
  },
  bubbleDateMine: {
    color: 'rgba(249,244,232,0.7)',
  },

  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 8,
    paddingHorizontal: 12,
    paddingTop: 8,
    paddingBottom: Platform.OS === 'ios' ? 8 : 12,
    borderTopWidth: 1,
    borderTopColor: 'rgba(126,102,58,0.25)',
    backgroundColor: 'rgba(0,0,0,0.2)',
  },
  input: {
    flex: 1,
    minHeight: 40,
    maxHeight: 120,
    backgroundColor: 'rgba(245,239,227,0.08)',
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 8,
    fontFamily: FONTS.regular,
    color: COLORS.parchment,
    fontSize: 14,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
  },
  sendBtn: {
    backgroundColor: COLORS.accent,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 18,
  },
  sendBtnDisabled: {
    opacity: 0.5,
  },
  sendBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.parchment,
    fontSize: 12,
    letterSpacing: 0.5,
  },
});
