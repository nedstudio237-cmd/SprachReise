import { useEffect, useState } from 'react';
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator,
  RefreshControl, Modal, TextInput, Alert, ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { COLORS, FONTS } from '../../constants/config';
import { adminAPI } from '../../services/api';

const STATUS_COLORS = {
  SENT: '#F59E0B',
  OPENED: '#3B82F6',
  REGISTERED: '#10B981',
  EXPIRED: '#6B7280',
};

const STATUS_LABELS = {
  SENT: 'Envoyée',
  OPENED: 'Ouverte',
  REGISTERED: 'Inscrit',
  EXPIRED: 'Expirée',
};

export default function AdminInvitationsScreen({ navigation }) {
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const [showModal, setShowModal] = useState(false);
  const [emailsText, setEmailsText] = useState('');
  const [template, setTemplate] = useState('FR');
  const [sending, setSending] = useState(false);

  const load = async () => {
    try {
      const res = await adminAPI.listInvitations();
      setInvitations(res.data || []);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Chargement impossible');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => { load(); }, []);

  const onRefresh = () => { setRefreshing(true); load(); };

  const openCompose = () => { setEmailsText(''); setTemplate('FR'); setShowModal(true); };
  const closeCompose = () => setShowModal(false);

  const sendCampaign = async () => {
    const emails = emailsText
      .split(/[\s,;\n]+/)
      .map((e) => e.trim())
      .filter((e) => e.length > 0);
    if (emails.length === 0) {
      Alert.alert('Liste vide', 'Entre au moins un email.');
      return;
    }
    setSending(true);
    try {
      const res = await adminAPI.sendInvitations(emails, template);
      Alert.alert(
        'Campagne lancée',
        `${res.data?.count ?? emails.length} invitations programmées pour envoi.${
          res.data?.skipped ? `\n${res.data.skipped} email(s) ignorés (doublons/invalides).` : ''
        }`,
      );
      closeCompose();
      setTimeout(load, 800);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Envoi impossible');
    } finally {
      setSending(false);
    }
  };

  const renderItem = ({ item }) => {
    const color = STATUS_COLORS[item.status] || COLORS.gold;
    const label = STATUS_LABELS[item.status] || item.status;
    return (
      <View style={styles.card}>
        <View style={styles.cardTop}>
          <Text style={styles.email} numberOfLines={1}>{item.email}</Text>
          <View style={[styles.badge, { backgroundColor: color + '22' }]}>
            <Text style={[styles.badgeText, { color }]}>{label}</Text>
          </View>
        </View>
        <Text style={styles.meta}>
          Envoyée le {new Date(item.sentAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })}
        </Text>
        <Text style={styles.meta}>
          Expire le {new Date(item.expiresAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })}
        </Text>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹ Retour</Text>
        </TouchableOpacity>
        <View style={styles.headerRow}>
          <Text style={styles.heading}>Invitations</Text>
          <TouchableOpacity style={styles.composeBtn} onPress={openCompose}>
            <Text style={styles.composeBtnText}>+ NOUVELLE</Text>
          </TouchableOpacity>
        </View>
        <Text style={styles.sub}>Envoi de campagnes aux formateurs potentiels</Text>
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.gold} />
        </View>
      ) : (
        <FlatList
          data={invitations}
          keyExtractor={(item) => String(item.id)}
          renderItem={renderItem}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={COLORS.gold} />}
          ListEmptyComponent={
            <Text style={styles.empty}>Aucune invitation envoyée pour le moment.</Text>
          }
        />
      )}

      <Modal visible={showModal} transparent animationType="slide" onRequestClose={closeCompose}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <ScrollView keyboardShouldPersistTaps="handled">
              <Text style={styles.modalTitle}>Nouvelle campagne</Text>
              <Text style={styles.modalSub}>
                Une à plusieurs adresses email, séparées par des virgules, espaces ou retours à la ligne.
              </Text>

              <Text style={styles.modalLabel}>Modèle d'invitation</Text>
              <View style={styles.tplRow}>
                {['FR', 'DE'].map((t) => (
                  <TouchableOpacity
                    key={t}
                    style={[styles.tplChip, template === t && styles.tplChipActive]}
                    onPress={() => setTemplate(t)}
                  >
                    <Text style={[styles.tplText, template === t && styles.tplTextActive]}>
                      {t === 'FR' ? '🇫🇷 Français' : '🇩🇪 Deutsch'}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Text style={styles.modalLabel}>Adresses email</Text>
              <TextInput
                style={[styles.modalInput, { minHeight: 130, textAlignVertical: 'top' }]}
                value={emailsText}
                onChangeText={setEmailsText}
                multiline
                placeholder="prof1@goethe.de, prof2@univ.fr&#10;prof3@example.com"
                placeholderTextColor={COLORS.muted}
                autoCapitalize="none"
                keyboardType="email-address"
              />
              <Text style={styles.hint}>
                💡 Chaque invitation contient un lien unique valide 7 jours. L'envoi se fait en arrière-plan.
              </Text>

              <View style={styles.modalActions}>
                <TouchableOpacity style={[styles.modalBtn, styles.modalCancel]} onPress={closeCompose} disabled={sending}>
                  <Text style={styles.modalCancelText}>ANNULER</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.modalBtn, styles.modalSend, sending && { opacity: 0.6 }]}
                  onPress={sendCampaign}
                  disabled={sending}
                >
                  {sending ? (
                    <ActivityIndicator color={COLORS.parchment} />
                  ) : (
                    <Text style={styles.modalConfirmText}>ENVOYER</Text>
                  )}
                </TouchableOpacity>
              </View>
            </ScrollView>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },

  topBar: { paddingHorizontal: 20, paddingTop: 16 },
  back: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14, marginBottom: 8 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  heading: { fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 26 },
  sub: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 13, marginTop: 4, marginBottom: 12, fontStyle: 'italic' },
  composeBtn: {
    backgroundColor: COLORS.accent,
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderRadius: 6,
  },
  composeBtnText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 11, letterSpacing: 1 },

  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  empty: { fontFamily: FONTS.regular, color: COLORS.muted, textAlign: 'center', marginTop: 60, fontSize: 14, fontStyle: 'italic' },

  list: { paddingHorizontal: 20, paddingBottom: 30 },

  card: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  email: { fontFamily: FONTS.uiMedium, color: COLORS.parchment, fontSize: 14, flex: 1, marginRight: 10 },
  badge: { paddingHorizontal: 9, paddingVertical: 3, borderRadius: 5 },
  badgeText: { fontFamily: FONTS.uiBold, fontSize: 10, letterSpacing: 0.5 },
  meta: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 12, marginTop: 2 },

  modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.7)', justifyContent: 'flex-end' },
  modalCard: { backgroundColor: COLORS.deep, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 24, maxHeight: '85%' },
  modalTitle: { fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 22, marginBottom: 4 },
  modalSub: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 13, marginBottom: 16 },
  modalLabel: { fontFamily: FONTS.uiBold, color: COLORS.muted, fontSize: 11, letterSpacing: 1.2, marginTop: 16, marginBottom: 8 },
  modalInput: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.3)',
    borderRadius: 6,
    padding: 12,
    color: COLORS.parchment,
    fontFamily: FONTS.regular,
    fontSize: 14,
  },
  hint: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 12, marginTop: 8, fontStyle: 'italic' },

  tplRow: { flexDirection: 'row', gap: 10 },
  tplChip: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 6,
    backgroundColor: 'rgba(249,244,232,0.06)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.3)',
    alignItems: 'center',
  },
  tplChipActive: { backgroundColor: COLORS.accent, borderColor: COLORS.accent },
  tplText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 13 },
  tplTextActive: { color: COLORS.parchment, fontFamily: FONTS.uiBold },

  modalActions: { flexDirection: 'row', gap: 10, marginTop: 24, marginBottom: 12 },
  modalBtn: { flex: 1, paddingVertical: 13, borderRadius: 6, alignItems: 'center' },
  modalCancel: { borderWidth: 1, borderColor: 'rgba(174,145,130,0.4)' },
  modalCancelText: { fontFamily: FONTS.uiBold, color: COLORS.muted, fontSize: 12, letterSpacing: 1 },
  modalSend: { backgroundColor: COLORS.accent },
  modalConfirmText: { fontFamily: FONTS.uiBold, color: COLORS.parchment, fontSize: 12, letterSpacing: 1 },
});
