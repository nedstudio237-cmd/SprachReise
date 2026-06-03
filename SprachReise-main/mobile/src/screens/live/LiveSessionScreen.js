import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { COLORS, FONTS } from '../../constants/config';

export default function LiveSessionScreen({ navigation, route }) {
  const { sessionId, title, agoraChannel, token, role } = route?.params || {};

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backLinkText}>‹ Retour</Text>
        </TouchableOpacity>

        <Text style={styles.heading}>Session live</Text>
        <Text style={styles.subheading}>{title || `Session #${sessionId}`}</Text>

        <View style={styles.placeholderBox}>
          <Text style={styles.placeholderEmoji}>📡</Text>
          <Text style={styles.placeholderTitle}>Salle vidéo (stub)</Text>
          <Text style={styles.placeholderText}>
            L'intégration Agora SDK n'est pas active dans cette version MVP.
            Les informations de connexion sont affichées ci-dessous.
          </Text>
        </View>

        <View style={styles.infoBlock}>
          <Text style={styles.infoLabel}>RÔLE</Text>
          <Text style={styles.infoValue}>{role || '—'}</Text>
        </View>

        <View style={styles.infoBlock}>
          <Text style={styles.infoLabel}>CANAL AGORA</Text>
          <Text style={styles.infoValueMono}>{agoraChannel || '—'}</Text>
        </View>

        <View style={styles.infoBlock}>
          <Text style={styles.infoLabel}>TOKEN</Text>
          <Text style={styles.infoValueMono} numberOfLines={3}>{token || '—'}</Text>
        </View>

        <TouchableOpacity style={styles.leaveBtn} onPress={() => navigation.goBack()}>
          <Text style={styles.leaveBtnText}>QUITTER</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.deep },
  scroll: { padding: 20, paddingBottom: 60 },

  backLinkText: { fontFamily: FONTS.uiMedium, color: COLORS.muted, fontSize: 14, marginBottom: 12 },

  heading: {
    fontFamily: FONTS.display,
    color: COLORS.parchment,
    fontSize: 24,
    marginBottom: 4,
  },
  subheading: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.cream,
    fontSize: 14,
    marginBottom: 20,
  },

  placeholderBox: {
    backgroundColor: 'rgba(184,137,58,0.12)',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(184,137,58,0.35)',
    padding: 24,
    alignItems: 'center',
    marginBottom: 24,
  },
  placeholderEmoji: { fontSize: 48, marginBottom: 12 },
  placeholderTitle: {
    fontFamily: FONTS.displayBold,
    color: COLORS.gold,
    fontSize: 18,
    marginBottom: 8,
  },
  placeholderText: {
    fontFamily: FONTS.regular,
    color: COLORS.cream,
    fontSize: 13,
    textAlign: 'center',
    lineHeight: 19,
  },

  infoBlock: {
    backgroundColor: 'rgba(245,239,227,0.06)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(126,102,58,0.25)',
    padding: 14,
    marginBottom: 12,
  },
  infoLabel: {
    fontFamily: FONTS.uiBold,
    color: COLORS.muted,
    fontSize: 10,
    letterSpacing: 1.4,
    marginBottom: 6,
  },
  infoValue: {
    fontFamily: FONTS.uiMedium,
    color: COLORS.parchment,
    fontSize: 14,
  },
  infoValueMono: {
    fontFamily: FONTS.ui,
    color: COLORS.parchment,
    fontSize: 13,
  },

  leaveBtn: {
    marginTop: 20,
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(239,68,68,0.5)',
    backgroundColor: 'rgba(239,68,68,0.10)',
  },
  leaveBtnText: {
    fontFamily: FONTS.uiBold,
    color: COLORS.error,
    fontSize: 13,
    letterSpacing: 1.2,
  },
});
