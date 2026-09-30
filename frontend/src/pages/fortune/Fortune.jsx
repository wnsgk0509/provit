import { useNavigate } from 'react-router-dom';

function Fortune() {
    const navigate = useNavigate();

    return (
        <div className="container my-4">
            <div className="card shadow-sm">
                <div className="card-header bg-white py-3">
                    <h3 className="card-title fw-bold mb-0">🔮 오늘의 운세</h3>
                </div>
                <div className="card-body">
                    <p className="text-muted">오늘의 취업 및 면접 운세를 확인하는 공간입니다. (운세 API 연동 예정)</p>
                    <div className="alert alert-primary mb-0" role="alert">
                        <strong>오늘의 한 줄 조언:</strong> 침착하게 준비한 역량을 발휘하면 좋은 기회가 찾아옵니다!
                    </div>
                </div>
            </div>

            <section className="card border-0 shadow-sm mt-4 overflow-hidden">
                <div className="card-body p-4 p-md-5 text-white" style={{ background: 'linear-gradient(135deg, #4f46e5, #7c3aed)' }}>
                    <span className="badge text-bg-light text-primary mb-3">취업 준비 성향 테스트</span>
                    <h3 className="fw-bold">나의 취업 MBTI는?</h3>
                    <p className="mb-4 opacity-75">12개의 질문으로 취업 준비와 면접 상황에서의 나의 성향을 알아보세요.</p>
                    <button type="button" className="btn btn-light fw-semibold text-primary px-4" onClick={() => navigate('/mbti')}>
                        MBTI 검사 시작하기
                    </button>
                </div>
            </section>
        </div>
    );
}

export default Fortune;
