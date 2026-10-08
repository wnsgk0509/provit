import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getInterviewAvailability } from '../../../api/interviewApi';
import { useAuth } from '../../../context/AuthContext';
import { useModal } from '../../../context/ModalContext';
import { clearInterviewProgress, MAINTENANCE_MESSAGE } from '../interviewProgress';

function InterviewAccessGate({ children }) {
    const navigate = useNavigate();
    const { user } = useAuth();
    const { showAlert } = useModal();
    const [allowed, setAllowed] = useState(false);
    const [error, setError] = useState(false);
    const redirectedRef = useRef(false);

    useEffect(() => {
        let active = true;
        let checking = false;
        let verified = false;
        let boundaryTimer;
        const block = async () => {
            if (!active || redirectedRef.current) return;
            redirectedRef.current = true;
            setAllowed(false);
            if (user?.userNum) clearInterviewProgress(user.userNum);
            await showAlert(MAINTENANCE_MESSAGE, { title: '점검 안내', type: 'warning' });
            navigate('/', { replace: true });
        };
        const check = async () => {
            if (checking || redirectedRef.current) return;
            checking = true;
            try {
                const availability = await getInterviewAvailability();
                if (!active) return;
                if (availability.maintenance) { block(); return; }
                window.clearTimeout(boundaryTimer);
                boundaryTimer = window.setTimeout(block,
                    Math.max(0, availability.nextChangeAt - availability.serverTime));
                setError(false);
                setAllowed(true);
                verified = true;
            } catch {
                if (active && !verified) setError(true);
            } finally { checking = false; }
        };
        const onVisible = () => { if (document.visibilityState === 'visible') void check(); };
        void check();
        const interval = window.setInterval(check, 60000);
        window.addEventListener('interview:maintenance', block);
        window.addEventListener('focus', check);
        document.addEventListener('visibilitychange', onVisible);
        return () => {
            active = false;
            window.clearInterval(interval);
            window.clearTimeout(boundaryTimer);
            window.removeEventListener('interview:maintenance', block);
            window.removeEventListener('focus', check);
            document.removeEventListener('visibilitychange', onVisible);
        };
    }, [navigate, user?.userNum]);

    if (error) return <p role="alert">모의면접 이용 가능 여부를 확인하지 못했습니다. 잠시 후 다시 접속해 주세요.</p>;
    if (!allowed) return <p>모의면접 이용 가능 여부를 확인하고 있습니다...</p>;
    return children;
}

export default InterviewAccessGate;
