import { useEffect, useState } from 'react';
import { clearMainResume, getMainResume, setMainResume } from '../api/documentApi';
import { useAuth } from '../context/AuthContext';

function errorMessage(error, fallback) {
    const response = error.response?.data;
    return (typeof response?.data === 'string' && response.data)
        || response?.responseCode?.message
        || fallback;
}

export default function useMainResume() {
    const { user, updateUser } = useAuth();
    const userNum = user?.userNum;
    const mainResumeNum = user?.mainResumeNum ?? null;
    const [reloadCount, setReloadCount] = useState(0);
    const [state, setState] = useState({
        loading: true, loadError: '', saving: false,
        pendingResumeNum: null, saveError: '', message: '',
    });

    useEffect(() => {
        let active = true;
        getMainResume().then((mainResume) => {
            if (mainResume === undefined) throw new Error('대표이력서 응답이 없습니다.');
            if (!active) return;
            setState((previous) => ({ ...previous, loading: false, loadError: '' }));
            updateUser((previous) => {
                if (previous?.userNum !== userNum || (previous.mainResumeNum ?? null) !== mainResumeNum) return previous;
                const savedResumeNum = mainResume?.resumeNum ?? null;
                return savedResumeNum === mainResumeNum ? previous : { ...previous, mainResumeNum: savedResumeNum };
            });
        }).catch((error) => {
            if (active) setState((previous) => ({
                ...previous, loading: false,
                loadError: errorMessage(error, '대표이력서 정보를 불러오지 못했습니다.'),
            }));
        });
        return () => { active = false; };
    }, [userNum, mainResumeNum, updateUser, reloadCount]);

    const reload = () => {
        setState((previous) => ({ ...previous, loading: true, loadError: '', saveError: '', message: '' }));
        setReloadCount((count) => count + 1);
    };

    const changeMainResume = async (resumeNum) => {
        if (state.loading || state.loadError || state.saving) return;
        setState((previous) => ({ ...previous, saving: true, pendingResumeNum: resumeNum, saveError: '', message: '' }));
        try {
            let mainResume = null;
            if (resumeNum === null) {
                await clearMainResume();
            } else {
                mainResume = await setMainResume(resumeNum);
                if (mainResume?.resumeNum == null) throw new Error('대표이력서 응답이 없습니다.');
            }
            updateUser((previous) => previous?.userNum === userNum
                ? { ...previous, mainResumeNum: mainResume?.resumeNum ?? null } : previous);
            setState((previous) => ({
                ...previous, saving: false, pendingResumeNum: null,
                message: resumeNum === null ? '대표이력서 지정을 해제했습니다.' : '대표이력서로 지정했습니다.',
            }));
        } catch (error) {
            setState((previous) => ({
                ...previous, saving: false, pendingResumeNum: null,
                saveError: errorMessage(error, '대표이력서를 변경하지 못했습니다. 다시 시도해 주세요.'),
            }));
        }
    };

    return {
        ...state, mainResumeNum, reload, changeMainResume,
        disabled: state.loading || Boolean(state.loadError) || state.saving,
        isMainResume: (resumeNum) => mainResumeNum != null
            && String(mainResumeNum) === String(resumeNum),
    };
}
