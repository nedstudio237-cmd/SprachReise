import { createStackNavigator } from '@react-navigation/stack';
import MainTabs from './MainTabs';
import CourseDetailScreen from '../screens/courses/CourseDetailScreen';
import QcmScreen from '../screens/qcm/QcmScreen';
import TrainerDetailScreen from '../screens/trainers/TrainerDetailScreen';
import EditTrainerProfileScreen from '../screens/trainers/EditTrainerProfileScreen';
import CreateCourseScreen from '../screens/trainers/CreateCourseScreen';
import MyCoursesScreen from '../screens/trainers/MyCoursesScreen';
import MyQcmsScreen from '../screens/trainers/MyQcmsScreen';
import MyQcmEditorScreen from '../screens/trainers/MyQcmEditorScreen';
import QcmResultsScreen from '../screens/trainers/QcmResultsScreen';
import MyExamsScreen from '../screens/trainers/MyExamsScreen';
import MyExamEditorScreen from '../screens/trainers/MyExamEditorScreen';
import ExamSubmissionsScreen from '../screens/trainers/ExamSubmissionsScreen';
import GradeSubmissionScreen from '../screens/trainers/GradeSubmissionScreen';
import MySessionsScreen from '../screens/trainers/MySessionsScreen';
import CreateSessionScreen from '../screens/trainers/CreateSessionScreen';
import LiveSessionScreen from '../screens/live/LiveSessionScreen';
import MyStudentsScreen from '../screens/trainers/MyStudentsScreen';
import MyStudentDetailScreen from '../screens/trainers/MyStudentDetailScreen';
import InboxScreen from '../screens/messages/InboxScreen';
import ChatScreen from '../screens/messages/ChatScreen';
import AdminApplicationsScreen from '../screens/admin/AdminApplicationsScreen';
import AdminInvitationsScreen from '../screens/admin/AdminInvitationsScreen';

const Stack = createStackNavigator();

export default function MainStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Tabs" component={MainTabs} />
      <Stack.Screen name="CourseDetail" component={CourseDetailScreen} />
      <Stack.Screen name="Qcm" component={QcmScreen} />
      <Stack.Screen name="TrainerDetail" component={TrainerDetailScreen} />
      <Stack.Screen name="EditTrainerProfile" component={EditTrainerProfileScreen} />
      <Stack.Screen name="CreateCourse" component={CreateCourseScreen} />
      <Stack.Screen name="MyCourses" component={MyCoursesScreen} />
      <Stack.Screen name="MyQcms" component={MyQcmsScreen} />
      <Stack.Screen name="MyQcmEditor" component={MyQcmEditorScreen} />
      <Stack.Screen name="QcmResults" component={QcmResultsScreen} />
      <Stack.Screen name="MyExams" component={MyExamsScreen} />
      <Stack.Screen name="MyExamEditor" component={MyExamEditorScreen} />
      <Stack.Screen name="ExamSubmissions" component={ExamSubmissionsScreen} />
      <Stack.Screen name="GradeSubmission" component={GradeSubmissionScreen} />
      <Stack.Screen name="MySessions" component={MySessionsScreen} />
      <Stack.Screen name="CreateSession" component={CreateSessionScreen} />
      <Stack.Screen name="LiveSession" component={LiveSessionScreen} />
      <Stack.Screen name="MyStudents" component={MyStudentsScreen} />
      <Stack.Screen name="MyStudentDetail" component={MyStudentDetailScreen} />
      <Stack.Screen name="Inbox" component={InboxScreen} />
      <Stack.Screen name="Chat" component={ChatScreen} />
      <Stack.Screen name="AdminApplications" component={AdminApplicationsScreen} />
      <Stack.Screen name="AdminInvitations" component={AdminInvitationsScreen} />
    </Stack.Navigator>
  );
}
