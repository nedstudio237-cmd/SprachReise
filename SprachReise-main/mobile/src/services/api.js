import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE_URL } from '../constants/config';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use(async (config) => {
  try {
    const token = await AsyncStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  } catch {}
  return config;
});

export const FILES_BASE = API_BASE_URL.replace(/\/api\/?$/, '/api/files/');

export function fileUrl(relativePath) {
  if (!relativePath) return null;
  if (/^https?:\/\//i.test(relativePath)) return relativePath;
  return FILES_BASE + relativePath.replace(/^\/+/, '');
}

export const authAPI = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (data) => api.post('/auth/register', data),
};

export const trainerApplicationsAPI = {
  submit: (fields, diploma) => {
    const form = new FormData();
    Object.entries(fields).forEach(([k, v]) => {
      if (v !== undefined && v !== null) form.append(k, String(v));
    });
    form.append('diploma', { uri: diploma.uri, name: diploma.name, type: diploma.type || 'application/pdf' });
    return api.post('/trainer-applications', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    });
  },
  getStatus: (id) => api.get(`/trainer-applications/${id}`),
  fromInvitation: (token) =>
    api.get('/trainer-applications/from-invitation', { params: { token } }),
};

export const trainersAPI = {
  list: () => api.get('/trainers'),
  getById: (userId) => api.get(`/trainers/${userId}`),
  getMe: () => api.get('/trainers/me'),
  updateMe: (data) => api.put('/trainers/me', data),
  uploadPhoto: (uri, name, type) => {
    const form = new FormData();
    form.append('file', { uri, name, type });
    return api.post('/trainers/me/photo', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    });
  },
};

export const qcmsAPI = {
  list: (levelId) => api.get('/qcms', { params: levelId ? { levelId } : {} }),
  getById: (id) => api.get(`/qcms/${id}`),
  getMine: () => api.get('/qcms/mine'),
  create: (payload) => api.post('/qcms', payload),
  update: (id, payload) => api.put(`/qcms/${id}`, payload),
  remove: (id) => api.delete(`/qcms/${id}`),
  getResults: (id) => api.get(`/qcms/${id}/results`),
  resultsPdfUrl: (id) => `${API_BASE_URL.replace(/\/$/, '')}/qcms/${id}/results.pdf`,
};

export const examsAPI = {
  getMine: () => api.get('/exams/mine'),
  getById: (id) => api.get(`/exams/${id}`),
  create: (payload) => api.post('/exams', payload),
  update: (id, payload) => api.put(`/exams/${id}`, payload),
  remove: (id) => api.delete(`/exams/${id}`),
  submit: (id, answerText) => api.post(`/exams/${id}/submit`, { answerText }),
  listSubmissions: (id) => api.get(`/exams/${id}/submissions`),
  grade: (subId, grade, feedback) => api.post(`/exams/submissions/${subId}/grade`, { grade, feedback }),
};

export const sessionsAPI = {
  list: (levelId) => api.get('/sessions', { params: levelId ? { levelId } : {} }),
  getById: (id) => api.get(`/sessions/${id}`),
  getMine: () => api.get('/sessions/mine'),
  create: (payload) => api.post('/sessions', payload),
  update: (id, payload) => api.put(`/sessions/${id}`, payload),
  cancel: (id) => api.delete(`/sessions/${id}`),
  start: (id) => api.post(`/sessions/${id}/start`),
  end: (id) => api.post(`/sessions/${id}/end`),
  join: (id) => api.post(`/sessions/${id}/join`),
  uploadAttachment: (id, uri, name, type) => {
    const form = new FormData();
    form.append('file', { uri, name, type: type || 'application/pdf' });
    return api.post(`/sessions/${id}/attachment`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    });
  },
};

export const coursesAPI = {
  list: (levelId) => api.get('/courses', { params: levelId ? { levelId } : {} }),
  getById: (id) => api.get(`/courses/${id}`),
  getMine: () => api.get('/courses/mine'),
  getStats: (id) => api.get(`/courses/${id}/stats`),
  create: ({ title, description, theme, status, publishAt, videoFile, pdfFile }) => {
    const form = new FormData();
    form.append('title', String(title || ''));
    form.append('description', String(description || ''));
    if (theme) form.append('theme', String(theme));
    if (status) form.append('status', String(status));
    if (publishAt) form.append('publishAt', String(publishAt));
    if (videoFile) {
      form.append('videoFile', {
        uri: videoFile.uri,
        name: videoFile.name,
        type: videoFile.type || 'video/mp4',
      });
    }
    if (pdfFile) {
      form.append('pdfFile', {
        uri: pdfFile.uri,
        name: pdfFile.name,
        type: pdfFile.type || 'application/pdf',
      });
    }
    return api.post('/courses', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000,
    });
  },
  update: (id, data) => api.put(`/courses/${id}`, data),
  remove: (id) => api.delete(`/courses/${id}`),
};

export const trainerStudentsAPI = {
  list: (status) => api.get('/trainer/students', { params: status ? { status } : {} }),
  getById: (learnerId) => api.get(`/trainer/students/${learnerId}`),
  stats: () => api.get('/trainer/students/stats'),
};

export const messagesAPI = {
  send: (recipientId, content) => api.post('/messages', { recipientId, content }),
  conversation: (otherUserId) => api.get(`/messages/conversation/${otherUserId}`),
  inbox: () => api.get('/messages/inbox'),
};

export const adminAPI = {
  listApplications: (status) =>
    api.get('/admin/applications', { params: status ? { status } : {} }),
  approveApplication: (id, assignedLevelCode, maxStudents) =>
    api.post(`/admin/applications/${id}/approve`, { assignedLevelCode, maxStudents }),
  rejectApplication: (id, motif) =>
    api.post(`/admin/applications/${id}/reject`, { motif }),
  diplomaUrl: (relativePath) =>
    relativePath ? `${API_BASE_URL.replace(/\/$/, '')}/files/${relativePath}` : null,

  listInvitations: () => api.get('/admin/invitations'),
  sendInvitations: (emails, template) =>
    api.post('/admin/invitations', { emails, template }),
};

export default api;
