import { useRef, useState } from 'react';
import { FileUp } from 'lucide-react';
import { createPortfolio } from '../../../api/documentApi';

const MAX_PORTFOLIO_FILE_SIZE = 20 * 1000 * 1000;
const PDF_SIGNATURE = [0x25, 0x50, 0x44, 0x46, 0x2d];

function PortfolioWrite() {
    const [portfolioTitle, setPortfolioTitle] = useState('');
    const [selectedFile, setSelectedFile] = useState(null);
    const [selectedFileName, setSelectedFileName] = useState('');
    const [fileError, setFileError] = useState('');
    const [isSaving, setIsSaving] = useState(false);
    const [saveMessage, setSaveMessage] = useState({ type: '', text: '' });
    const fileInputRef = useRef(null);

    const handleFileChange = async (event) => {
        const fileInput = event.currentTarget;
        const selectedFile = event.target.files?.[0];

        if (!selectedFile) {
            setSelectedFile(null);
            setSelectedFileName('');
            setFileError('');
            return;
        }

        const isPdf = selectedFile.type === 'application/pdf'
            || selectedFile.name.toLowerCase().endsWith('.pdf');

        if (!isPdf) {
            event.target.value = '';
            setSelectedFile(null);
            setSelectedFileName('');
            setFileError('PDF 형식의 파일만 선택할 수 있습니다.');
            return;
        }

        if (selectedFile.size > MAX_PORTFOLIO_FILE_SIZE) {
            event.target.value = '';
            setSelectedFile(null);
            setSelectedFileName('');
            setFileError('포트폴리오 파일은 20MB 이하여야 합니다.');
            return;
        }

        try {
            const signatureBuffer = await selectedFile.slice(0, PDF_SIGNATURE.length).arrayBuffer();
            const signatureBytes = new Uint8Array(signatureBuffer);
            const hasPdfSignature = PDF_SIGNATURE.every(
                (expectedByte, index) => signatureBytes[index] === expectedByte,
            );

            if (fileInput.files?.[0] !== selectedFile) return;

            if (!hasPdfSignature) {
                fileInput.value = '';
                setSelectedFile(null);
                setSelectedFileName('');
                setFileError('올바른 PDF 파일이 아닙니다. PDF 파일을 다시 선택해 주세요.');
                return;
            }
        } catch {
            if (fileInput.files?.[0] !== selectedFile) return;
            fileInput.value = '';
            setSelectedFile(null);
            setSelectedFileName('');
            setFileError('파일을 확인하지 못했습니다. 다시 선택해 주세요.');
            return;
        }

        setSelectedFile(selectedFile);
        setSelectedFileName(selectedFile.name);
        setFileError('');
        setSaveMessage({ type: '', text: '' });
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!selectedFile) {
            setFileError('포트폴리오 PDF 파일을 선택해 주세요.');
            return;
        }

        setIsSaving(true);
        setSaveMessage({ type: '', text: '' });

        try {
            const savedPortfolio = await createPortfolio(portfolioTitle, selectedFile);
            const portfolioNum = savedPortfolio?.portfolioNum;
            setPortfolioTitle('');
            setSelectedFile(null);
            setSelectedFileName('');
            setFileError('');
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
            setSaveMessage({
                type: 'success',
                text: portfolioNum
                    ? `포트폴리오가 저장되었습니다. (파일명: ${portfolioNum}.pdf)`
                    : '포트폴리오가 저장되었습니다.',
            });
        } catch (error) {
            const responseData = error.response?.data;
            setSaveMessage({
                type: 'error',
                text: responseData?.data
                    || responseData?.responseCode?.message
                    || '포트폴리오를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
            });
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <form className="document-form" onSubmit={handleSubmit}>
            <div className="document-form-heading">
                <span>PORTFOLIO</span>
                <h2>포트폴리오 등록</h2>
                <p>프로젝트와 실무 역량을 보여줄 포트폴리오 파일을 등록하세요.</p>
            </div>

            <section className="document-form-section" aria-labelledby="portfolio-form-title">
                <div className="document-section-heading">
                    <h3 id="portfolio-form-title">포트폴리오 정보</h3>
                    <span>문서 제목과 제출할 파일을 확인해 주세요.</span>
                </div>
                <div className="document-field-grid">
                    <div className="document-field document-field-wide">
                        <label htmlFor="portfolioTitle">포트폴리오 제목 <b>*</b></label>
                        <input id="portfolioTitle" value={portfolioTitle} onChange={(event) => setPortfolioTitle(event.target.value)} maxLength="200" placeholder="예: 백엔드 개발 프로젝트 포트폴리오" required />
                    </div>
                    <div className="document-field document-field-wide">
                        <label htmlFor="portfolioFile">포트폴리오 파일 <b>*</b></label>
                        <label
                            className={`document-file-upload${selectedFile ? ' is-valid' : ''}`}
                            htmlFor="portfolioFile"
                        >
                            <FileUp size={30} aria-hidden="true" />
                            <strong>{selectedFileName || 'PDF 파일을 선택해 주세요.'}</strong>
                            <span>20MB 이하의 PDF 파일만 등록할 수 있습니다.</span>
                        </label>
                        <input ref={fileInputRef} id="portfolioFile" className="document-file-input" type="file" accept="application/pdf,.pdf" onChange={handleFileChange} required />
                        {fileError && <p className="document-file-error" role="alert">{fileError}</p>}
                    </div>
                </div>
            </section>

            <div className="document-form-actions">
                <button type="submit" className="document-primary-button" disabled={isSaving}>
                    {isSaving ? '저장 중...' : '포트폴리오 저장'}
                </button>
            </div>
            {saveMessage.text && (
                <p className={`document-save-message is-${saveMessage.type}`} role="status">
                    {saveMessage.text}
                </p>
            )}
        </form>
    );
}

export default PortfolioWrite;
