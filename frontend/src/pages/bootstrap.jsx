/**
 * [팀원 공통 부트스트랩 페이지 템플릿 (bootstrap.jsx)]
 *
 * 📌 사용 방법:
 * 1. 새 페이지를 만들 때 이 파일을 복사하여 원하는 폴더(예: src/pages/기능명/MyPage.jsx)에 넣습니다.
 * 2. 컴포넌트 이름 `BootstrapTemplate`을 해당 페이지명(예: `JobDetail`)으로 변경합니다.
 * 3. 아래 예시로 작성된 Container, Grid(Row/Col), Card, Form, Table 중 필요한 부분만 남기고 내용을 채워 넣으세요.
 */

function BootstrapTemplate() {
    return (
        <div className="container my-4">
            {/* 1. 페이지 헤더 영역 */}
            <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom">
                <div>
                    <h2 className="fw-bold mb-1">페이지 제목</h2>
                    <p className="text-muted mb-0">페이지에 대한 간단한 설명을 작성하세요.</p>
                </div>
                <div>
                    <button className="btn btn-primary me-2">주요 액션 버튼</button>
                    <button className="btn btn-outline-secondary">보조 버튼</button>
                </div>
            </div>

            {/* 2. 알림창 (Alert) 예시 */}
            <div className="alert alert-info alert-dismissible fade show" role="alert">
                <strong>안내:</strong> 팀원들이 참고할 수 있는 기본 부트스트랩 양식입니다.
            </div>

            {/* 3. 그리드 & 카드 (Grid & Card) 레이아웃 예시 */}
            <div className="row g-4 mb-4">
                {/* 좌측 메인 영역 (8칸) */}
                <div className="col-12 col-lg-8">
                    <div className="card shadow-sm mb-4">
                        <div className="card-header bg-white py-3">
                            <h5 className="card-title mb-0 fw-bold">카드 제목 1 (폼 양식)</h5>
                        </div>
                        <div className="card-body">
                            <form onSubmit={(e) => e.preventDefault()}>
                                <div className="mb-3">
                                    <label htmlFor="exampleInput" className="form-label">
                                        입력 레이블
                                    </label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        id="exampleInput"
                                        placeholder="내용을 입력하세요"
                                    />
                                </div>
                                <div className="mb-3">
                                    <label htmlFor="exampleSelect" className="form-label">
                                        선택 옵션
                                    </label>
                                    <select className="form-select" id="exampleSelect">
                                        <option defaultValue>옵션을 선택하세요</option>
                                        <option value="1">옵션 1</option>
                                        <option value="2">옵션 2</option>
                                    </select>
                                </div>
                                <div className="mb-3 form-check">
                                    <input type="checkbox" className="form-check-input" id="exampleCheck" />
                                    <label className="form-check-label" htmlFor="exampleCheck">
                                        체크박스 확인
                                    </label>
                                </div>
                                <button type="submit" className="btn btn-primary">
                                    저장하기
                                </button>
                            </form>
                        </div>
                    </div>

                    {/* 테이블 양식 예시 */}
                    <div className="card shadow-sm">
                        <div className="card-header bg-white py-3">
                            <h5 className="card-title mb-0 fw-bold">데이터 목록 (테이블)</h5>
                        </div>
                        <div className="card-body p-0">
                            <div className="table-responsive">
                                <table className="table table-hover align-middle mb-0">
                                    <thead className="table-light">
                                        <tr>
                                            <th scope="col">#</th>
                                            <th scope="col">항목</th>
                                            <th scope="col">상태</th>
                                            <th scope="col">관리</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <th scope="row">1</th>
                                            <td>샘플 데이터 1</td>
                                            <td>
                                                <span className="badge bg-success">완료</span>
                                            </td>
                                            <td>
                                                <button className="btn btn-sm btn-outline-primary">상세보기</button>
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">2</th>
                                            <td>샘플 데이터 2</td>
                                            <td>
                                                <span className="badge bg-warning text-dark">진행중</span>
                                            </td>
                                            <td>
                                                <button className="btn btn-sm btn-outline-primary">상세보기</button>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 우측 사이드바 영역 (4칸) */}
                <div className="col-12 col-lg-4">
                    <div className="card shadow-sm mb-4">
                        <div className="card-header bg-white py-3">
                            <h5 className="card-title mb-0 fw-bold">사이드 정보 카드</h5>
                        </div>
                        <div className="card-body">
                            <p className="card-text text-muted">
                                우측 영역에 요약 정보나 필터, 통계 등을 배치할 수 있습니다.
                            </p>
                            <ul className="list-group list-group-flush">
                                <li className="list-group-item d-flex justify-content-between align-items-center">
                                    항목 A<span className="badge bg-primary rounded-pill">14</span>
                                </li>
                                <li className="list-group-item d-flex justify-content-between align-items-center">
                                    항목 B<span className="badge bg-primary rounded-pill">2</span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default BootstrapTemplate;
