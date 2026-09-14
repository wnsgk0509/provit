function Fortune() {
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
        </div>
    );
}

export default Fortune;
