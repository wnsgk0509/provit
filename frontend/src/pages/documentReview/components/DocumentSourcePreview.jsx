import { FileText, LoaderCircle, RotateCcw } from 'lucide-react';
import { Link } from 'react-router-dom';
import { documentPreviewSections } from '../documentReviewConfig';

function DocumentSourcePreview({ documentType, documentLabel, documentNum, document, isLoading, error, onRetry }) {
    if (isLoading)
        return (
            <p className="review-status" role="status">
                <LoaderCircle className="review-spinner" size={18} aria-hidden="true" /> 문서 내용을 불러오고 있습니다.
            </p>
        );
    if (error)
        return (
            <div className="review-status is-error" role="alert">
                <p>{error}</p>
                <button className="review-text-button" type="button" onClick={onRetry}>
                    <RotateCcw size={14} aria-hidden="true" /> 다시 시도
                </button>
            </div>
        );
    if (!document)
        return (
            <div className="review-source-empty">
                <FileText size={27} aria-hidden="true" />
                <p>문서를 선택하면 원문을 확인할 수 있습니다.</p>
            </div>
        );

    return (
        <div className="review-source">
            <div className="review-source-heading">
                <strong>선택한 문서</strong>
                <Link to={`/documents/${documentType}/${documentNum}`}>문서 상세 보기</Link>
            </div>
            {documentType === 'portfolio' ? (
                <div className="review-pdf-source">
                    <span className="review-file-icon">
                        <FileText size={24} aria-hidden="true" />
                    </span>
                    <div>
                        <strong>{document.originalFileName || `${document.portfolioTitle || '포트폴리오'}.pdf`}</strong>
                        <p>PDF의 프로젝트 설명과 시각 자료를 첨삭 대상으로 사용합니다.</p>
                    </div>
                </div>
            ) : (
                <details className="review-source-preview">
                    <summary>{documentLabel} 원문 확인</summary>
                    <div
                        className="review-source-sections"
                        role="region"
                        aria-label={`${documentLabel} 원문`}
                        tabIndex={0}
                    >
                        {documentPreviewSections(documentType, document).map((section) => (
                            <section key={section.title}>
                                <h3>{section.title}</h3>
                                <p>{section.content}</p>
                            </section>
                        ))}
                    </div>
                </details>
            )}
        </div>
    );
}

export default DocumentSourcePreview;
