import { useCallback, useEffect, useState } from 'react';
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator,
  RefreshControl, Modal, TextInput, Alert, ScrollView, Linking,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { COLORS, FONTS, LEVELS } from '../../constants/config';
import { adminAPI } from '../../services/api';

const STATUSES = [
  { key: 'PENDING', label: 'En attente', color: '#F59E0B' },
  { key: 'APPROVED', label: 'Validées', color: '#10B981' },
  { key: 'REJECTED', label: 'Refusées', color: '#EF4444' },
];

export default function AdminApplicationsScreen({ navigation }) {
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const [selected, setSelected] = useState(null);
  const [mode, setMode] = useState(null); // 'approve' | 'reject' | null
  const [assignedLevel, setAssignedLevel] = useState('A1');
  const [maxStudents, setMaxStudents] = useState('30');
  const [motif, setMotif] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const load = useCallback(async () => {
    try {
      const res = await adminAPI.listApplications(statusFilter);
      setApplications(res.data || []);
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Chargement impossible');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [statusFilter]);

  useEffect(() => { setLoading(true); load(); }, [load]);

  const onRefresh = () => { setRefreshing(true); load(); };

  const openApprove = (app) => {
    setSelected(app);
    setAssignedLevel(app.requestedLevelCode || 'A1');
    setMaxStudents('30');
    setMode('approve');
  };

  const openReject = (app) => {
    setSelected(app);
    setMotif('');
    setMode('reject');
  };

  const close = () => { setSelected(null); setMode(null); };

  const confirmApprove = async () => {
    const ms = parseInt(maxStudents, 10);
    if (isNaN(ms) || ms < 1) {
      Alert.alert('Quota invalide', 'Le quota doit être un nombre positif.');
      return;
    }
    setActionLoading(true);
    try {
      await adminAPI.approveApplication(selected.id, assignedLevel, ms);
      Alert.alert(
        'Candidature validée',
        `${selected.firstName} ${selected.lastName} a été créé(e) comme formateur niveau ${assignedLevel}. Un email avec le mot de passe temporaire a été envoyé (consultez les logs backend).`,
      );
      close();
      load();
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Validation impossible');
    } finally {
      setActionLoading(false);
    }
  };

  const confirmReject = async () => {
    if (!motif.trim()) {
      Alert.alert('Motif requis', 'Indique la raison du refus.');
      return;
    }
    setActionLoading(true);
    try {
      await adminAPI.rejectApplication(selected.id, motif.trim());
      Alert.alert('Candidature refusée', 'Un email de notification a été envoyé.');
      close();
      load();
    } catch (e) {
      Alert.alert('Erreur', e.response?.data?.error || e.message || 'Refus impossible');
    } finally {
      setActionLoading(false);
    }
  };

  const renderItem = ({ item }) => {
    const status = STATUSES.find((s) => s.key === item.status);
    return (
      <View style={styles.card}>
        <View style={styles.cardTop}>
          <Text style={styles.name}>{item.firstName} {item.lastName}</Text>
          <View style={[styles.badge, { backgroundColor: (status?.color || COLORS.gold) + '22' }]}>
            <Text style={[styles.badgeText, { color: status?.color || COLORS.gold }]}>
              {item.requestedLevelCode}
            </Text>
          </View>
        </View>
        <Text style={styles.meta}>📧 {item.email}</Text>
        {item.phone ? <Text style={styles.meta}>📞 {item.phone}</Text> : null}
        {item.nativeLanguage ? <Text style={styles.meta}>🗣️ {item.nativeLanguage}</Text> : null}

        {item.bio ? (
          <View style={styles.block}>
            <Text style={styles.blockLabel}>BIOGRAPHIE</Text>
            <Text style={styles.blockText}>{item.bio}</Text>
          </View>
        ) : null}

        {item.motivation ? (
          <View style={styles.block}>
            <Text style={styles.blockLabel}>MOTIVATION</Text>
            <Text style={styles.blockText}>{item.motivation}</Text>
          </View>
        ) : null}

        {item.diplomaPdfPath ? (
          <TouchableOpacity
            style={styles.diplomaBtn}
            onPress={() => {
              const url = adminAPI.diplomaUrl(item.diplomaPdfPath);
              if (url) Linking.openURL(url).catch(() => Alert.alert('Erreur', 'Impossible d\'ouvrir le PDF'));
            }}
          >
            <Text style={styles.diplomaBtnText}>📄 Voir le diplôme</Text>
          </TouchableOpacity>
        ) : null}

        {item.reviewMotif ? (
          <View style={[styles.block, { borderColor: 'rgba(239,68,68,0.3)', backgroundColor: 'rgba(239,68,68,0.05)' }]}>
            <Text style={[styles.blockLabel, { color: '#EF4444' }]}>MOTIF DU REFUS</Text>
            <Text style={styles.blockText}>{item.reviewMotif}</Text>
          </View>
        ) : null}

        <Text style={styles.date}>
          Soumise le {new Date(item.submittedAt).toLocaleDateString('fr-FR', {
            day: '2-digit', month: 'short', year: 'numeric',
          })}
        </Text>

        {item.status === 'PENDING' && (
          <View style={styles.actions}>
            <TouchableOpacity
              style={[styles.actionBtn, styles.rejectBtn]}
              onPress={() => openReject(item)}
            >
              <Text style={styles.rejectBtnText}>REFUSER</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.actionBtn, styles.approveBtn]}
              onPress={() => openApprove(item)}
            >
              <Text style={styles.approveBtnText}>VALIDER</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.back}>‹ Retour</Text>
        </TouchableOpacity>
        <Text style={styles.heading}>Candidatures formateurs</Text>
      </View>

      <View style={styles.filterRow}>
        {STATUSES.map((s) => (
          <TouchableOpacity
            key={s.key}
            style={[styles.filterChip, statusFilter === s.key && styles.filterChipActive]}
            onPress={() => setStatusFilter(s.key)}
          >
            <Text style={[styles.filterText, statusFilter === s.key && styles.filterTextActive]}>
              {s.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.gold} />
        </View>
      ) : (
        <FlatList
          data={applications}
          keyExtractor={(item) => String(item.id)}
          renderItem={renderItem}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={COLORS.gold} />}
          ListEmptyComponent={
            <Text style={styles.empty}>Aucune candidature {statusFilter === 'PENDING' ? 'en attente' : statusFilter === 'APPROVED' ? 'validée' : 'refusée'}.</Text>
          }
        />
      )}

      <Modal visible={mode !== null} transparent animationType="slide" onRequestClose={close}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <ScrollView keyboardShouldPersistTaps="handled">
              <Text style={styles.modalTitle}>
                {mode === 'approve' ? 'Valider la candidature' : 'Refuser la candidature'}
              </Text>
              {selected && (
                <Text style={styles.modalSub}>
                  {selected.firstName} {selected.lastName} — {selected.email}
                </Text>
              )}

              {mode === 'approve' && (
                <>
                  <Text style={styles.modalLabel}>Niveau d'enseignement assigné</Text>
                  <View style={styles.levelsRow}>
                    {LEVELS.map((lvl) => (
                      <TouchableOpacity
                        key={lvl}
                        style={[styles.levelChip, assignedLevel === lvl && styles.levelChipActive]}
                        onPress={() => setAssignedLevel(lvl)}
                      >
                        <Text style={[styles.levelText, assignedLevel === lvl && styles.levelTextActive]}>
                          {lvl}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Text style={styles.modalLabel}>Quota d'apprenants</Text>
                  <TextInput
                    style={styles.modalInput}
                    value={maxStudents}
                    onChangeText={setMaxStudents}
                    keyboardType="number-pad"
                    placeholder="30"
                    placeholderTextColor={COLORS.muted}
                  />
                </>
              )}

              {mode === 'reject' && (
                <>
                  <Text style={styles.modalLabel}>Motif du refus *</Text>
                  <TextInput
                    style={[styles.modalInput, { minHeight: 90, textAlignVertical: 'top' }]}
                    value={motif}
                    onChangeText={setMotif}
                    multiline
                    placeholder="Expliquez la raison du refus..."
                    placeholderTextColor={COLORS.muted}
                  />
                </>
              )}

              <View style={styles.modalActions}>
                <TouchableOpacity style={[styles.modalBtn, styles.modalCancel]} onPress={close} disabled={actionLoading}>
                  <Text style={styles.modalCancelText}>ANNULER</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.modalBtn,
                    mode === 'approve' ? styles.modalApprove : styles.modalReject,
                    actionLoading && { opacity: 0.6 },
                  ]}
                  onPress={mode === 'approve' ? confirmApprove : confirmReject}
                  disabled={actionLoading}
                >
                  {actionLoading ? (
                    <ActivityIndicator color={COLORS.parchment} />
                  ) : (
                    <Text style={styles.modalConfirmText}>
                      {mode === 'approve' ? 'CONFIRMER' : 'REFUSER'}
                    </Text>
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
  heading: { fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 26, marginBottom: 8 },

  filterRow: { flexDirection: 'row', paddingHorizontal: 20, gap: 8, marginBottom: 12 },
  filterChip: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 20,
    backgroundColor: 'rgba(245,239,227,0.07)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
  },
  filterChipActive: { backgroundColor: COLORS.accent, borderColor: COLORS.accent },
  filterText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 12 },
  filterTextActive: { color: COLORS.parchment },

  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  empty: {
    fontFamily: FONTS.regular,
    color: COLORS.muted,
    textAlign: 'center',
    marginTop: 60,
    fontSize: 14,
    fontStyle: 'italic',
  },

  list: { paddingHorizontal: 20, paddingBottom: 30 },

  card: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 10,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  name: { fontFamily: FONTS.displayBold, color: COLORS.parchment, fontSize: 17 },
  badge: { paddingHorizontal: 9, paddingVertical: 4, borderRadius: 5 },
  badgeText: { fontFamily: FONTS.uiBold, fontSize: 11, letterSpacing: 0.5 },

  meta: { fontFamily: FONTS.regular, color: COLORS.cream, fontSize: 13, marginTop: 2 },

  block: {
    marginTop: 12,
    padding: 10,
    backgroundColor: 'rgba(126,102,58,0.08)',
    borderRadius: 6,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.2)',
  },
  blockLabel: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 10, letterSpacing: 1, marginBottom: 4 },
  blockText: { fontFamily: FONTS.regular, color: COLORS.cream, fontSize: 13, lineHeight: 19 },

  diplomaBtn: {
    marginTop: 12,
    paddingVertical: 10,
    borderWidth: 1,
    borderColor: COLORS.gold,
    borderRadius: 6,
    alignItems: 'center',
  },
  diplomaBtnText: { fontFamily: FONTS.uiBold, color: COLORS.gold, fontSize: 12, letterSpacing: 0.5 },

  date: { fontFamily: FONTS.ui, color: COLORS.muted, fontSize: 11, marginTop: 10, fontStyle: 'italic' },

  actions: { flexDirection: 'row', gap: 10, marginTop: 14 },
  actionBtn: { flex: 1, paddingVertical: 11, borderRadius: 6, alignItems: 'center' },
  approveBtn: { backgroundColor: '#10B981' },
  approveBtnText: { fontFamily: FONTS.uiBold, color: '#fff', fontSize: 12, letterSpacing: 1 },
  rejectBtn: { backgroundColor: 'transparent', borderWidth: 1, borderColor: '#EF4444' },
  rejectBtnText: { fontFamily: FONTS.uiBold, color: '#EF4444', fontSize: 12, letterSpacing: 1 },

  modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.7)', justifyContent: 'flex-end' },
  modalCard: {
    backgroundColor: COLORS.deep,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
    maxHeight: '85%',
  },
  modalTitle: { fontFamily: FONTS.display, color: COLORS.parchment, fontSize: 22, marginBottom: 4 },
  modalSub: { fontFamily: FONTS.regular, color: COLORS.muted, fontSize: 13, marginBottom: 20 },
  modalLabel: {
    fontFamily: FONTS.uiBold,
    color: COLORS.muted,
    fontSize: 11,
    letterSpacing: 1.2,
    marginTop: 16,
    marginBottom: 8,
  },
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
  levelsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  levelChip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 6,
    backgroundColor: 'rgba(249,244,232,0.06)',
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.3)',
  },
  levelChipActive: { backgroundColor: COLORS.accent, borderColor: COLORS.accent },
  levelText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 13 },
  levelTextActive: { color: COLORS.parchment, fontFamily: FONTS.uiBold },

  modalActions: { flexDirection: 'row', gap: 10, marginTop: 28, marginBottom: 12 },
  modalBtn: { flex: 1, paddingVertical: 13, borderRadius: 6, alignItems: 'center' },
  modalCancel: { borderWidth: 1, borderColor: 'rgba(174,145,130,0.4)' },
  modalCancelText: { fontFamily: FONTS.uiBold, color: COLORS.muted, fontSize: 12, letterSpacing: 1 },
  modalApprove: { backgroundColor: '#10B981' },
  modalReject: { backgroundColor: '#EF4444' },
  modalConfirmText: { fontFamily: FONTS.uiBold, color: '#fff', fontSize: 12, letterSpacing: 1 },
});
