package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

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
		
		Assertions.assertThat(result).extracting("username").containsExactly("member1","member2");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
