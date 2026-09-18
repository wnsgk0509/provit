import './interview.css';



function Interview() {
    return <div className="interview-page">
        <p>AI MOCK INTERVIEW</p>
        <h1><b>모의면접</b></h1>
        <br />

        <div className="interview-explain">
            <span>1. 질문은 마이페이지에서 업로드한 유저의 서류 기반 질문 3개와 후속질문 2개로 구성되어있습니다. </span><br />
            <span>2. 유저는 면접 스타일과 제출할 문서를 지정하여 면접을 진행합니다.</span><br />
            <span>3. 면접이 종료되면 유저는 AI로부터 점수와 총평을 받을 수 있고 마이페이지에 기록됩니다.</span>
        </div>

        <div className='interview-container'>

            <div className='interview-contents'>
                <div className='interview-explain-time'><span>약 15분 소요</span></div><br />
                <p>1. 문서 옵션</p>
                <p>2. 질의 응답</p>
                <p>3. 결과 해설</p>
                <hr />
                <p>분석 항목</p>
                <p>자신감 논리성 전문성 전달력 직무적합성</p>
            </div>

            <div className='interview-progress'>
                <h2><b>면접 커스텀</b></h2>
                <form action="" method=''>
                    <span>포트폴리오 : </span>
                    <select name="interview-portfolio" id="interview-portfolio">
                        <option value="portfolio1">업로드한 포트폴리오1</option>
                        <option value="portfolio2">업로드한 포트폴리오2</option>
                    </select><br />
                    <span>자기소개서 : </span>
                    <select name="interview-coverLetter" id="interview-coverLetter">
                        <option value="coverLetter1">업로드한 자기소개서1</option>
                        <option value="coverLetter2">업로드한 자기소개서2</option>
                    </select><br />
                    <span>면접스타일 : </span>
                    <select name="interview-style" id="interview-style">
                        <option value="randomInterview">랜덤면접</option>
                        <option value="oneByOneInterview">일대일면접</option>
                        <option value="panelInterview">다대일면접</option>
                        <option value="groupInterview">다대다면접</option>
                    </select><br />

                    <span>면접 난이도 : </span>
                    <label htmlFor='hard'><input type="radio" name='interview-level' id='hard' value="hard"/>압박면접</label>
                    <label htmlFor='normal'><input type="radio" name='interview-level' id='normal' value="normal"/>심층면접</label>
                    <label htmlFor='easy'><input type="radio" name='interview-level' id='easy' value="easy"/>일반면접</label>
                    <br />
                    <button className="btn btn-primary" type="button">면접 시작</button>
                </form>
            </div>

        </div>



    </div>
}

export default Interview;
