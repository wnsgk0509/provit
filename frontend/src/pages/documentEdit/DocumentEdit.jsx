import { useEffect, useState } from 'react';
import { ArrowLeft, FileSearch } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getCoverLetter, getResume } from '../../api/documentApi';
import CoverLetterWrite from '../documentWrite/components/CoverLetterWrite';
import ResumeWrite from '../documentWrite/components/ResumeWrite';
import '../documentWrite/DocumentWrite.css';
import '../documentRead/DocumentRead.css';

const editConfigs = {
    resume: {
        label: '이력서',
        load: getResume,
        component: ResumeWrite,
    },
    'cover-letter': {
        label: '자기소개서',
        load: getCoverLetter,
        component: CoverLetterWrite,
    },
};

function DocumentEdit() {
    const { documentType, documentId } = useParams();
    const navigate = useNavigate();
    const config = editConfigs[documentType];
    const readPath = `/documents/${documentType}/${documentId}`;
    const [document, setDocument] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
    }, []);

    useEffect(() => {
        let isActive = true;
        const currentConfig = editConfigs[documentType];

        async function loadDocument() {
            if (!currentConfig || !/^\d+$/.test(documentId) || Number(documentId) < 1) {
                if (isActive) {
                    setDocument(null);
                    setErrorMessage('수정할 수 없는 문서 주소입니다.');
                    setIsLoading(false);
                }
                return;
            }

            setIsLoading(true);
            setErrorMessage('');
            setDocument(null);
            try {
                const loadedDocument = await currentConfig.load(documentId);
                if (isActive) setDocument(loadedDocument);
            } catch (error) {
                if (!isActive) return;
                const responseData = error.response?.data;
                setErrorMessage(
                    responseData?.data
                    || responseData?.responseCode?.message
                    || '수정할 문서를 불러오지 못했습니다.',
                );
            } finally {
                if (isActive) setIsLoading(false);
            }
        }

        loadDocument();
        return () => {
            isActive = false;
        };
    }, [documentType, documentId]);

    const SelectedForm = config?.component;
    const returnToRead = () => navigate(readPath, { replace: true });

    return (
        <div className="document-write-page document-edit-page">
            {config && (
                <Link className="document-read-back" to={readPath}>
                    <ArrowLeft size={17} /> {config.label} 조회로 돌아가기
                </Link>
            )}

            <header className="document-write-header">
                <span>DOCUMENT EDIT</span>
                <h1>{config ? `${config.label} 수정` : '문서 수정'}</h1>
                <p>저장된 문서 내용을 수정하고 변경사항을 저장하세요.</p>
            </header>

            {isLoading && (
                <div className="document-read-status" role="status">
                    <span className="document-read-spinner" aria-hidden="true" />
                    <p>문서를 불러오고 있습니다.</p>
                </div>
            )}

            {!isLoading && errorMessage && (
                <div className="document-read-status is-error" role="alert">
                    <FileSearch size={32} aria-hidden="true" />
                    <strong>문서를 수정할 수 없습니다.</strong>
                    <p>{errorMessage}</p>
                    <Link to="/mypage">문서 목록으로 이동</Link>
                </div>
            )}

            {!isLoading && !errorMessage && document && SelectedForm && (
                <SelectedForm
                    initialData={document}
                    onCancel={returnToRead}
                    onSaved={returnToRead}
                />
            )}
        </div>
    );
}

export default DocumentEdit;
