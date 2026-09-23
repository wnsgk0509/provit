import { useState } from 'react';
import { Download, FileText } from 'lucide-react';
import { downloadPortfolioFile } from '../../../api/documentApi';
import { DocumentDate } from './ResumeRead';

function PortfolioRead({ document }) {
    const [isDownloading, setIsDownloading] = useState(false);
    const [downloadError, setDownloadError] = useState('');

    const handleDownload = async () => {
        setIsDownloading(true);
        setDownloadError('');
        try {
            const fileBlob = await downloadPortfolioFile(document.portfolioNum);
            const downloadUrl = URL.createObjectURL(fileBlob);
            const anchor = window.document.createElement('a');
            anchor.href = downloadUrl;
            anchor.download = `${document.portfolioTitle || `portfolio-${document.portfolioNum}`}.pdf`;
            window.document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            URL.revokeObjectURL(downloadUrl);
        } catch (error) {
            setDownloadError(
                error.response?.data?.data
                || '파일을 다운로드하지 못했습니다. 잠시 후 다시 시도해 주세요.',
            );
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <article className="document-form document-read-card">
            <div className="document-form-heading document-read-heading">
                <div>
                    <span>PORTFOLIO</span>
                    <h2>{document.portfolioTitle || '-'}</h2>
                    <p>등록한 포트폴리오 PDF 파일을 확인하세요.</p>
                </div>
                <DocumentDate createdAt={document.createdAt} updatedAt={document.updatedAt} />
            </div>

            <section className="document-form-section" aria-labelledby="portfolio-read-title">
                <div className="document-section-heading">
                    <h3 id="portfolio-read-title">첨부 파일</h3>
                    <span>PDF 파일</span>
                </div>
                <div className="document-read-file">
                    <span className="document-read-file-icon" aria-hidden="true"><FileText size={30} /></span>
                    <div>
                        <strong>{document.portfolioTitle || `포트폴리오 ${document.portfolioNum}`}.pdf</strong>
                        <span>포트폴리오 첨부 파일</span>
                    </div>
                    <button type="button" onClick={handleDownload} disabled={isDownloading}>
                        <Download size={17} /> {isDownloading ? '다운로드 중...' : '다운로드'}
                    </button>
                </div>
                {downloadError && <p className="document-file-error" role="alert">{downloadError}</p>}
            </section>
        </article>
    );
}

export default PortfolioRead;
