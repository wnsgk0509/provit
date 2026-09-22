import { useState } from 'react';
import { FileUp } from 'lucide-react';

function PortfolioWrite() {
    const [portfolioTitle, setPortfolioTitle] = useState('');
    const [selectedFileName, setSelectedFileName] = useState('');
    const [fileError, setFileError] = useState('');

    const handleFileChange = (event) => {
        const selectedFile = event.target.files?.[0];

        if (!selectedFile) {
            setSelectedFileName('');
            setFileError('');
            return;
        }

        const isPdf = selectedFile.type === 'application/pdf'
            || selectedFile.name.toLowerCase().endsWith('.pdf');

        if (!isPdf) {
            event.target.value = '';
            setSelectedFileName('');
            setFileError('PDF 형식의 파일만 선택할 수 있습니다.');
            return;
        }

        setSelectedFileName(selectedFile.name);
        setFileError('');
    };

    return (
        <form className="document-form" onSubmit={(event) => event.preventDefault()}>
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
                        <label className="document-file-upload" htmlFor="portfolioFile">
                            <FileUp size={30} aria-hidden="true" />
                            <strong>{selectedFileName || 'PDF 파일을 선택해 주세요.'}</strong>
                            <span>PDF 형식의 파일만 등록할 수 있습니다.</span>
                        </label>
                        <input id="portfolioFile" className="document-file-input" type="file" accept="application/pdf,.pdf" onChange={handleFileChange} required />
                        {fileError && <p className="document-file-error" role="alert">{fileError}</p>}
                    </div>
                </div>
            </section>

            <div className="document-form-actions">
                <button type="submit" className="document-primary-button">포트폴리오 저장</button>
            </div>
        </form>
    );
}

export default PortfolioWrite;
