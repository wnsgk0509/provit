import { useState } from 'react';
import { FileText } from 'lucide-react';
import ResumeWrite from './components/ResumeWrite';
import CoverLetterWrite from './components/CoverLetterWrite';
import PortfolioWrite from './components/PortfolioWrite';
import './DocumentWrite.css';

const documentOptions = [
    { value: 'resume', label: '이력서' },
    { value: 'coverLetter', label: '자기소개서' },
    { value: 'portfolio', label: '포트폴리오' },
];

const documentForms = {
    resume: ResumeWrite,
    coverLetter: CoverLetterWrite,
    portfolio: PortfolioWrite,
};

function DocumentWrite() {
    const [documentType, setDocumentType] = useState('resume');
    const SelectedDocumentForm = documentForms[documentType];

    return (
        <div className="document-write-page">
            <header className="document-write-header">
                <span>DOCUMENT</span>
                <h1>취업 문서 작성</h1>
                <p>작성할 문서를 선택하고 필요한 정보를 입력해 주세요.</p>
            </header>

            <section className="document-type-card" aria-labelledby="document-type-title">
                <div className="document-type-icon" aria-hidden="true">
                    <FileText size={22} />
                </div>
                <div className="document-type-content">
                    <label id="document-type-title" htmlFor="documentType">문서 종류</label>
                    <select
                        id="documentType"
                        value={documentType}
                        onChange={(event) => setDocumentType(event.target.value)}
                    >
                        {documentOptions.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                    <p>선택한 문서에 맞는 작성 항목이 아래에 표시됩니다.</p>
                </div>
            </section>

            <SelectedDocumentForm />
        </div>
    );
}

export default DocumentWrite;
