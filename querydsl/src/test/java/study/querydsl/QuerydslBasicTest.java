package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@Autowired
	EntityManager em;
	
	JPAQueryFactory queryFactory;
	
	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);
		
		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);

	}
	
	@Test
	public void startJPQL() {
		//member1을 찾아라
		Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getSingleResult();
		
		Assertions.assertThat(findByJPQL.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void startQuerydsl() {
		
//		QMember m = new QMember("m");
//		QMember m = QMember.member;
		
		Member findMember = queryFactory.select(member).from(member).where(member.username.eq("member1")).fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
		
	}
	
	@Test
	public void search() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1").and(member.age.eq(10))).fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
			
	}
	
	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(
					member.username.eq("member1"),
					member.age.eq(10)  //and 조건,동적쿼리 짜기 편리함.
					)
			.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
			
	}
	
	@Test
	public void resultFetch() {
		
		// 다건
		List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();
		
		// 단건 조회
		Member fetchOne = queryFactory
			.selectFrom(member)
			.fetchOne();
		
		
		Member fetchFirst = queryFactory
			.selectFrom(member)
			.fetchFirst();
		
		// total count 쿼리 2번, 페이징용 쿼리
		QueryResults<Member> fetchResults = queryFactory
			.selectFrom(member)
			.fetchResults();
		
		fetchResults.getTotal();
		List<Member> content = fetchResults.getResults();
		
		// total
		long total = queryFactory
			.selectFrom(member)
			.fetchCount();
		
	}
	
	// 정렬
	@Test
	public void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();
		
		
		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);
		
		Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
		Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
		Assertions.assertThat(memberNull.getUsername()).isNull();
		
	}
	
	@Test
	public void paging1() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();
		
		Assertions.assertThat(result.size()).isEqualTo(2);
	}
	
	@Test
	public void paging2() {
		 QueryResults<Member> fetchResults = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetchResults();
		
		Assertions.assertThat(fetchResults.getTotal()).isEqualTo(4);
		Assertions.assertThat(fetchResults.getLimit()).isEqualTo(2);
		Assertions.assertThat(fetchResults.getOffset()).isEqualTo(1);
		Assertions.assertThat(fetchResults.getResults().size()).isEqualTo(2);
	}
	
	@Test
	public void aggregation() {
		List<Tuple> result = queryFactory
			.select(member.count(),
						member.age.sum(),
						member.age.avg(),
						member.age.max(),
						member.age.min()
			)
			.from(member)
			.fetch();
		
		Tuple tuple = result.get(0);
		
		Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
		Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
		Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);

	}
	
	// 팀의 이름과 각 팀의 평균 연령을 구하라
	@Test
	public void group() throws Exception {
		List<Tuple> result = queryFactory
			.select(team.name,member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();
		
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		
		Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
		Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);  // (10 + 20 ) /2
		
		Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
		Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);  // (30 + 40 ) /2
			
	}
	
	// 팀 A에 소속된 모든 회원
	@Test
	public void join() {
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();
		
		System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@");
		Assertions.assertThat(result).extracting("username").containsExactly("member1","member2");
	}
	
	// 세타 조인
	// 회원의 이름이 팀 이름과 같은 회원 조회
	@Test
	public void theta_join() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		
		List<Member> result = queryFactory
			.select(member)
			.from(member.team)
			.where(member.username.eq(team.name))
			.fetch();
		
		Assertions.assertThat(result).extracting("username").containsExactly("teamA","teamB");
	}
	
	// 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
	// JPQA : select m, t from Member m left join m.team t on t.name= 'teamA'
	@Test
	public void join_on_filtering() {
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team).on(team.name.eq("teamA"))
			.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple" + tuple);
		}
	}
	
	// 연관관계 없는 엔티티 외부 조인
	// 회원의 이름이 팀 이름과 같은 대상 외부 조인
	@Test
	public void join_on_no_relation() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));
		
		 List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name)) //필터링으로 조인
			.fetch();
		
			for (Tuple tuple : result) {
				System.out.println("tuple" + tuple);
			}
	}
	
	@PersistenceUnit
	EntityManagerFactory emf;
	
	@Test
	public void fetchJoinNo() {
		em.flush();
		em.clear();
		
		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1"))
				.fetchOne();
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
	}
	
	@Test
	public void fetchJoinUse() {
		em.flush();
		em.clear();
		
		Member findMember = queryFactory
				.selectFrom(member)
				.join(member.team, team).fetchJoin()
				.where(member.username.eq("member1"))
				.fetchOne();
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
	}
	
	// 나이가 가장 많은 회원 조회 (서브 쿼리)
	@Test
	public void subQuery() {
		
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
					JPAExpressions
						.select(memberSub.age.max())
						.from(memberSub)
			))
			.fetch();
		
		Assertions.assertThat(result).extracting("age").containsExactly(40);
	}
	
	// 나이가 평균 이상인 회원 조회 (서브 쿼리)
	@Test
	public void subQueryGoe() {
		
		QMember memberSub = new QMember("memberSub");
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.goe(
					JPAExpressions
						.select(memberSub.age.avg())
						.from(memberSub)
			))
			.fetch();
		
		Assertions.assertThat(result).extracting("age").containsExactly(30,40);
	}
	
	
	@Test
	public void selectSubQuery() {
		QMember memberSub = new QMember("memberSub");
		
		List<Tuple> result = queryFactory
			.select(member.username,
					JPAExpressions
						.select(memberSub.age.avg())
						.from(memberSub)
			)
			.from(member)
			.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple" + tuple);
		}
	}
	
	@Test
	public void basicCase() {
		List<String> result = queryFactory
			.select(member.age
					.when(10).then("열살")
					.when(20).then("스무살")
					.otherwise("기타"))
			.from(member)
			.fetch();
		
		for(String s : result) {
			System.out.println("s =" + s);
		}
	}
	
	@Test
	public void complexCase() {
		List<String> result = queryFactory
			.select(new CaseBuilder()
					.when(member.age.between(0, 20)).then("0 ~ 20 살")
					.when(member.age.between(21, 30)).then("21~30살")
					.otherwise("기타"))
			.from(member)
			.fetch();
		
		for(String s : result) {
			System.out.println("s =" + s);
		}
	}
	
	@Test
	public void constant() {
		List<Tuple> result = queryFactory
			.select(member.username, Expressions.constant("A"))
			.from(member)
			.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple " + tuple);
		}
	}
	
	@Test
	public void concat() {
		List<String> result = queryFactory
			.select(member.username.concat("_").concat(member.age.stringValue()))
			.from(member)
			.where(member.username.eq("member1"))
			.fetch();
		
		for(String s : result) {
			System.out.println("s =" + s);
		}
	}
	
	// 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함 
	@Test
	public void findDtoByJPQL() {
		List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m",MemberDto.class)
			.getResultList();
		
		for(MemberDto dto : result) {
			System.out.println("memberDto = " + dto);
		}
	}
	
	
	// GETTER, SETTER에 값을 대입 방식
	@Test
	public void findDtoBySetter() {
		List<MemberDto> result = queryFactory
			.select(Projections.bean(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for(MemberDto dto : result) {
			System.out.println("memberDto = " + dto);
		}
	}
	
	// 생성된 필드에 바로 값을 대입
	// 필드명이 같아아햠
	@Test
	public void findDtoByField() {
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for(MemberDto dto : result) {
			System.out.println("memberDto = " + dto);
		}
	}
	
	// 생성자는 이름이 아닌 타입이 맞아야한다.
	@Test
	public void findDtoByConstructor() {
		List<MemberDto> result = queryFactory
			.select(Projections.constructor(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for(MemberDto dto : result) {
			System.out.println("memberDto = " + dto);
		}
	}
	
	// as 사용하여 값 대입
	// subquery사용
	@Test
	public void findUserDto() {
		QMember memberSub = new QMember("memberSub");
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class,
					member.username.as("name"),
					ExpressionUtils.as(JPAExpressions
						.select(memberSub.age.max())
							.from(memberSub), "age")
			))
			.from(member)
			.fetch();
		
		for(UserDto dto : result) {
			System.out.println("UserDto = " + dto);
		}
	}
	
	// 동적 쿼리
	@Test
	public void dynamicQuery_BooleanBuilder() {
		String usernameParam = "member1";
		Integer ageParam  = 10;
		
		List<Member> result = searchMember1(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}
	
	private List<Member> searchMember1(String usernameCond, Integer ageCond){
		
	 	BooleanBuilder builder = new BooleanBuilder();
		
		if(usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}
		if(ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}
		
		return queryFactory
			.selectFrom(member)
			.where(builder)
			.fetch();
	}
}
