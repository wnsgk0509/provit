// Static examples are displayed only after an explicit "결과 예시 보기" action.
const DOCUMENT_EXAMPLES = {
    resume: {
        summary: '경력의 핵심 업무는 드러나지만, 본인의 역할과 성과를 더 구체적으로 설명하면 강점이 분명해집니다.',
        strengths: ['지원 직무와 연결되는 개발 경험을 담고 있습니다.', '주요 업무를 경력 항목에 정리했습니다.'],
        improvements: [
            {
                section: '경력 · 주요 업무',
                title: '업무 나열에 본인의 역할 더하기',
                issue: '담당한 업무의 범위와 개선 결과가 드러나지 않습니다.',
                original: '백엔드 개발 및 API 개선 업무를 담당했습니다.',
                suggestion:
                    '주문 API의 백엔드 개발을 담당하고, 응답 지연의 원인을 분석해 조회 로직을 개선했습니다. [확인 가능한 개선 결과를 추가해 주세요.]',
                reason: '역할, 행동, 결과의 순서로 정리하면 기여도를 이해하기 쉽습니다. 수치는 실제로 확인할 수 있을 때만 추가하세요.',
            },
        ],
    },
    'cover-letter': {
        summary:
            '경험을 통해 성장하려는 태도가 드러납니다. 구체적인 상황과 행동을 중심으로 서술하면 이야기가 더 설득력 있어집니다.',
        strengths: ['문제 해결 경험을 자신의 강점과 연결했습니다.', '경험에서 얻은 배움을 설명하려는 방향이 좋습니다.'],
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
            '프로젝트의 목적과 사용 기술을 소개하고 있습니다. 구현 과정에서 본인이 내린 판단과 기여를 더하면 프로젝트의 가치가 잘 전달됩니다.',
        strengths: ['프로젝트의 목적을 소개했습니다.', '사용한 기술을 정리해 구현 환경을 파악할 수 있습니다.'],
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
            '서류 전체에서 개발 경험과 문제 해결 역량이 드러납니다. 경력 기간의 표현을 맞추고, 각 경험에서 맡은 역할과 실제 성과를 구체적으로 연결하면 지원자의 강점이 더 분명해집니다.',
        strengths: [
            '이력서와 자기소개서가 같은 지원 직무를 중심으로 작성되어 있습니다.',
            '문제 해결 경험을 여러 서류에서 설명하고 있습니다.',
        ],
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
            summary: '백엔드 개발 직무의 취업 준비 추천 예시입니다. 실제 미보유 여부나 채용 필수 조건을 뜻하지 않습니다.',
            coverageNote: '화면 구성을 위한 정적 예시이며 선택한 서류를 분석한 결과가 아닙니다.',
            recommendations: [
                {
                    category: 'experience',
                    title: 'API 자동화 테스트 경험',
                    reason: '정상·오류 상황을 검증한 경험으로 API 안정성을 설명할 수 있습니다.',
                    action: '작은 API에 단위·통합 테스트를 작성하고 검증한 실패 사례와 개선 내용을 정리해 보세요.',
                },
                {
                    category: 'skill',
                    title: 'Docker 기반 실행 환경 구성',
                    reason: '서버 실행 환경을 재현하는 과정으로 배포와 운영에 대한 이해를 보여줄 수 있습니다.',
                    action: '직접 구현한 서버를 Docker로 실행하고 환경 변수와 실행 절차를 문서화해 보세요.',
                },
                {
                    category: 'certification',
                    title: 'SQLD 학습·자격 검토',
                    reason: '데이터 모델과 SQL 기초 지식을 정리하는 선택적 학습 목표입니다.',
                    action: '지원 공고의 우대 조건과 학습 목표를 확인하고 공식 안내에서 응시 조건을 검토해 보세요.',
                },
                {
                    category: 'qualification',
                    title: '프로젝트 결과물과 기술 문서 공개',
                    reason: '직접 구현한 결과물과 판단 근거를 함께 정리하면 경험을 설명하기 쉽습니다.',
                    action: '공개 가능한 저장소에 README, 본인 역할과 기술 선택 이유를 정리해 보세요.',
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
