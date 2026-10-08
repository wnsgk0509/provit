import { useState } from 'react';
import { Download, FileText, Trash2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { deletePortfolio, downloadPortfolioFile } from '../../../api/documentApi';
import { DocumentDate } from './ResumeRead';
import { useModal } from '../../../context/ModalContext';

function PortfolioRead({ document }) {
    const navigate = useNavigate();
    const { showConfirm, showToast, showAlert } = useModal();
    const [isDownloading, setIsDownloading] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [downloadError, setDownloadError] = useState('');
    const [deleteError, setDeleteError] = useState('');

    const handleDownload = async () => {
        setIsDownloading(true);
        setDownloadError('');
        try {
            const fileBlob = await downloadPortfolioFile(document.portfolioNum);
            const downloadUrl = URL.createObjectURL(fileBlob);
            const anchor = window.document.createElement('a');
            anchor.href = downloadUrl;
            anchor.download = document.originalFileName || `${document.portfolioTitle || `portfolio-${document.portfolioNum}`}.pdf`;
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

    const handleDelete = async () => {
        const ok = await showConfirm('포트폴리오를 삭제하시겠습니까?\n첨부된 PDF도 함께 삭제되며 복구할 수 없습니다.', {
            title: '포트폴리오 삭제',
            type: 'warning',
            confirmText: '삭제',
            cancelText: '취소',
            isDestructive: true
        });
        if (!ok) return;

        setIsDeleting(true);
        setDeleteError('');
        try {
            await deletePortfolio(document.portfolioNum);
            showToast('포트폴리오가 삭제되었습니다.', 'info');
            navigate('/mypage', { replace: true });
            window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'auto' }));
        } catch (error) {
            const responseData = error.response?.data;
            const errMsg = responseData?.data
                || responseData?.responseCode?.message
                || '포트폴리오를 삭제하지 못했습니다.';
            setDeleteError(errMsg);
            showAlert(errMsg, { type: 'error' });
        } finally {
            setIsDeleting(false);
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
                        <strong>{document.originalFileName || `${document.portfolioTitle || `포트폴리오 ${document.portfolioNum}`}.pdf`}</strong>
                        <span>포트폴리오 첨부 파일</span>
                    </div>
                    <button type="button" onClick={handleDownload} disabled={isDownloading}>
                        <Download size={17} /> {isDownloading ? '다운로드 중...' : '다운로드'}
                    </button>
                </div>
                {downloadError && <p className="document-file-error" role="alert">{downloadError}</p>}
            </section>

            <div className="document-form-actions">
                <button type="button" className="document-danger-button" onClick={handleDelete} disabled={isDeleting}>
                    <Trash2 size={17} /> {isDeleting ? '삭제 중...' : '포트폴리오 삭제'}
                </button>
            </div>
            {deleteError && <p className="document-delete-error" role="alert">{deleteError}</p>}
        </article>
    );
}

export default PortfolioRead;
