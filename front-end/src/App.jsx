import React from 'react';
import {Routes,Route,Navigate} from 'react-router-dom';
import Layout from './components/Layout';
import RoleGuard from './components/RoleGuard';
import Login from './pages/Login';
import Signup from './pages/Signup';
import CreateCompany from './pages/CreateCompany';
import ForgotPassword from './pages/ForgotPassword';
import Invitation from './pages/Invitation';
import Dashboard from './pages/Dashboard';
import Requests from './pages/Requests';
import NewRequest from './pages/NewRequest';
import RequestDetail from './pages/RequestDetail';
import Calendar from './pages/Calendar';
import Teams from './pages/Teams';
import People from './pages/People';
import Invitations from './pages/Invitations';
import Balances from './pages/Balances';
import Statistics from './pages/Statistics';
import Notifications from './pages/Notifications';
import History from './pages/History';
import Profile from './pages/Profile';
import Company from './pages/Company';
import Settings from './pages/Settings';
import NotFound from './pages/NotFound';

export default function App(){return <Routes>
<Route path='/' element={<Navigate to='/login' replace/>}/>
<Route path='/login' element={<Login/>}/>
<Route path='/signup' element={<Signup/>}/>
<Route path='/creer-entreprise' element={<CreateCompany/>}/>
<Route path='/mot-de-passe-oublie' element={<ForgotPassword/>}/>
<Route path='/invitation' element={<Invitation/>}/>
<Route element={<RoleGuard role='rh'/>}><Route path='/rh' element={<Layout/>}>
<Route index element={<Navigate to='dashboard' replace/>}/>
<Route path='dashboard' element={<Dashboard/>}/>
<Route path='demandes' element={<Requests/>}/>
<Route path='nouvelle-demande' element={<NewRequest/>}/>
<Route path='demandes/:id' element={<RequestDetail/>}/>
<Route path='demandes/:id/modifier' element={<NewRequest/>}/>
<Route path='calendrier' element={<Calendar/>}/>
<Route path='soldes' element={<Balances/>}/>
<Route path='historique' element={<History/>}/>
<Route path='notifications' element={<Notifications/>}/>
<Route path='profil' element={<Profile/>}/>
<Route path='validations' element={<Requests validation/>}/>
<Route path='statistiques' element={<Statistics/>}/>
<Route path='entreprise' element={<Company/>}/>
<Route path='equipes' element={<Teams/>}/>
<Route path='employes' element={<People/>}/>
<Route path='invitations' element={<Invitations/>}/>
<Route path='parametres' element={<Settings/>}/>
</Route></Route>
<Route element={<RoleGuard role='manager'/>}><Route path='/manager' element={<Layout/>}>
<Route index element={<Navigate to='dashboard' replace/>}/>
<Route path='dashboard' element={<Dashboard/>}/>
<Route path='demandes' element={<Requests/>}/>
<Route path='nouvelle-demande' element={<NewRequest/>}/>
<Route path='demandes/:id' element={<RequestDetail/>}/>
<Route path='demandes/:id/modifier' element={<NewRequest/>}/>
<Route path='calendrier' element={<Calendar/>}/>
<Route path='soldes' element={<Balances/>}/>
<Route path='historique' element={<History/>}/>
<Route path='notifications' element={<Notifications/>}/>
<Route path='profil' element={<Profile/>}/>
<Route path='validations' element={<Requests validation/>}/>
<Route path='statistiques' element={<Statistics/>}/>
<Route path='equipe' element={<People/>}/>
</Route></Route>
<Route element={<RoleGuard role='employe'/>}><Route path='/employe' element={<Layout/>}>
<Route index element={<Navigate to='dashboard' replace/>}/>
<Route path='dashboard' element={<Dashboard/>}/>
<Route path='demandes' element={<Requests/>}/>
<Route path='nouvelle-demande' element={<NewRequest/>}/>
<Route path='demandes/:id' element={<RequestDetail/>}/>
<Route path='demandes/:id/modifier' element={<NewRequest/>}/>
<Route path='calendrier' element={<Calendar/>}/>
<Route path='soldes' element={<Balances/>}/>
<Route path='historique' element={<History/>}/>
<Route path='notifications' element={<Notifications/>}/>
<Route path='profil' element={<Profile/>}/>
</Route></Route>
<Route path='*' element={<NotFound/>}/>
</Routes>}
