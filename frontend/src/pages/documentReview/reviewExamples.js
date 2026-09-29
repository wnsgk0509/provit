// Static examples are displayed only after an explicit "결과 예시 보기" action.
const DOCUMENT_EXAMPLES = {
    resume: {
        summary: '담당 기능과 본인 책임 범위, 실제 구현·검증 내용이 빠져 있어 수행 역량을 판단하기 어렵습니다. 직접 맡은 업무와 확인한 결과를 먼저 보완해 주세요.',
        strengths: [],
        improvements: [
            {
                section: '경력 · 주요 업무',
                title: '업무 나열에 본인의 역할 더하기',
                issue: '백엔드 개발과 API 개선이라는 업무명만 있어 어느 기능을 직접 맡았고 어떤 문제를 어떤 판단으로 개선했는지 확인되지 않습니다.',
                original: '백엔드 개발 및 API 개선 업무를 담당했습니다.',
                suggestion:
                    '[직접 담당한 API 기능]의 백엔드 개발을 담당했습니다. [실제로 확인한 문제]에 대해 [원인 확인 과정과 개선 판단]을 바탕으로 [실제로 적용한 수정]을 수행하고, [실제 검증 방법과 확인 결과]를 점검했습니다.',
                reason: '팀 전체 업무와 본인의 구현 범위를 먼저 구분하고 실제 문제와 조치 근거를 보완하세요. 수치 없이도 오류 재현·동작 확인 결과로 설명할 수 있으며, 대괄호를 확인하지 않은 사실로 채우면 안 됩니다.',
            },
        ],
    },
    'cover-letter': {
        summary:
            '어떤 문제가 있었는지와 본인이 수행한 행동이 없어 문제 해결 과정과 기여를 판단하기 어렵습니다. 실제 상황, 역할, 선택 이유와 확인 결과 순서로 보완해 주세요.',
        strengths: [],
        improvements: [
            {
                section: '문제 해결 경험',
                title: '추상적인 표현을 행동으로 바꾸기',
                issue: '열심히 노력했다는 표현만으로는 문제를 해결한 과정을 알기 어렵습니다.',
                original: '프로젝트에서 어려움이 있었지만 팀원들과 열심히 노력해서 해결했습니다.',
                suggestion:
                    '프로젝트에서 [구체적인 문제]가 발생했을 때, 저는 [담당한 역할]을 맡아 원인을 확인했습니다. 팀원들과 [실제로 수행한 해결 방법]을 적용해 [확인된 결과]를 얻었습니다.',
                reason: '상황, 본인의 행동, 결과를 구분하면 경험의 근거가 선명해집니다. 대괄호에는 실제 경험을 작성하세요.',
            },
        ],
    },
    portfolio: {
        summary:
            '기술명과 프로젝트 소개만 있어 직접 맡은 기능과 요구사항, 기술 선택 이유를 확인하기 어렵습니다. 담당 기능과 구현 판단, 동작을 검증한 근거를 구분해 보완해 주세요.',
        strengths: [],
        improvements: [
            {
                section: '프로젝트 소개',
                title: '기술 목록에 선택 이유와 기여 더하기',
                issue: '기술 목록만으로는 본인의 역할과 기술을 선택한 이유가 드러나지 않습니다.',
                original: 'Spring과 React를 활용해 취업 지원 서비스를 개발했습니다.',
                suggestion:
                    '취업 준비 문서를 관리하는 서비스를 개발했습니다. 저는 [담당 기능]을 구현하며 [요구사항]을 해결하기 위해 Spring과 React를 선택했습니다. [기술 선택의 이유와 실제 기여 내용을 추가해 주세요.]',
                reason: '문제, 기술 선택, 본인의 역할을 함께 설명하면 프로젝트를 통해 보여줄 역량이 구체적으로 드러납니다.',
            },
        ],
    },
};

export function createBundleReviewExample(includePortfolio) {
    return {
        summary:
            '경력 기간이 서류마다 다르게 표현되어 사실관계 확인이 우선 필요합니다. 경험의 담당 범위·문제 상황·판단·검증 결과도 부족해 직무 수행 역량을 평가하기 어렵습니다. 기간을 확인한 뒤 실제 맡은 역할과 조치 효과의 근거를 보완해 주세요.',
        strengths: [],
        documentReviews: {
            resume: DOCUMENT_EXAMPLES.resume,
            coverLetter: DOCUMENT_EXAMPLES['cover-letter'],
            portfolio: includePortfolio ? DOCUMENT_EXAMPLES.portfolio : null,
        },
        careerPreparation: {
            occupationCode: '2',
            occupationName: 'IT개발·데이터',
            jobCode: '84',
            jobName: '백엔드/서버개발',
            summary: 'API 자동화 테스트 수행 경험과 실행 환경 재현 기술이 서류에서 확인되지 않을 때의 보강 예시입니다. 자격증은 선택적 항목이며 실제 수행 경험을 대신하지 않습니다.',
            coverageNote: '화면 구성을 위한 정적 예시이며 선택한 서류를 분석한 결과가 아닙니다.',
            recommendations: [
                {
                    category: 'experience',
                    title: 'API 자동화 테스트 경험',
                    reason: '요청 검증을 구현했지만 테스트 수행 경험이 기재되지 않았다면, 정상·오류·경계 조건을 검증하는 경험을 통해 동작을 확인하는 수행 범위를 보강할 수 있습니다.',
                    action: '아직 수행하지 않았다면 정상 요청·필수값 누락·잘못된 형식·경계값의 기대 응답을 정의하고 자동화 테스트를 구현하세요. 실제 응답과 재실행 결과를 확인하세요. 이미 수행했다면 실제 시나리오와 결과를 기재하세요.',
                },
                {
                    category: 'skill',
                    title: 'Docker 이미지·컨테이너 구성 역량',
                    reason: '실행 환경을 재현하는 기술이 서류에서 확인되지 않는다면 이미지·환경 변수·네트워크 설정 역량을 보강할 수 있습니다. 이는 테스트 수행 경험과 별개의 기술 범위입니다.',
                    action: '이미지와 컨테이너의 차이, Dockerfile, 환경 변수와 네트워크 설정을 학습·실습하세요. 새 환경에서 같은 설정으로 서버와 필요한 연결이 동작하는지 확인하세요. 이미 활용했다면 실제 설정·검증 근거를 기재하세요.',
                },
                {
                    category: 'certification',
                    title: 'SQLD 자격 취득 검토',
                    reason: '데이터 모델·SQL 지식의 외부 검증 이력이 필요할 때 검토하는 선택적 자격입니다. 취업 필수 조건이나 API 구현·검증 경험의 대체 수단이 아닙니다.',
                    action: '공식 안내에서 평가 범위와 응시 조건을 확인해 취득 여부를 결정하세요. 해당 범위의 문제를 직접 풀고 부족한 영역을 점검하세요. 실제 API 수행·검증 경험의 보완을 먼저 진행하세요.',
                },
            ],
        },
        consistencyIssues: [
            {
                type: 'mismatch',
                title: '경력 기간의 표현을 확인해 주세요.',
                sources: [
                    { documentType: 'resume', section: '경력 1', text: '근무 기간: 2024년 3월 ~ 2025년 2월' },
                    {
                        documentType: 'cover-letter',
                        section: '문제 해결 경험',
                        text: '해당 회사에서 2년간 근무했습니다.',
                    },
                ],
                recommendation:
                    '같은 회사의 경험을 설명한 것인지 확인하고, 실제 근무 기간에 맞게 두 서류의 표현을 통일해 주세요.',
            },
            ...(includePortfolio
                ? [
                      {
                          type: 'needsConfirmation',
                          title: '프로젝트 역할의 범위를 확인해 주세요.',
                          sources: [
                              {
                                  documentType: 'cover-letter',
                                  section: '문제 해결 경험',
                                  text: '프로젝트 전체를 주도했습니다.',
                              },
                              {
                                  documentType: 'portfolio',
                                  section: '역할 소개 · 3페이지',
                                  text: '담당 역할: 백엔드 API 개발',
                              },
                          ],
                          recommendation:
                              '전체 진행을 주도하면서 개발도 담당했다면, 운영 역할과 구현 역할을 구분해 설명해 주세요. 두 표현만으로 사실관계의 충돌을 단정할 수는 없습니다.',
                      },
                  ]
                : []),
        ],
    };
}
