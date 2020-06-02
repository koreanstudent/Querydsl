package study.querydsl.repository;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

public class MemberRepositoryImpl implements MemberRepositoryCustom{
	
	private final JPAQueryFactory queryFactory;
	
	public MemberRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}
	
	@Override
	public List<MemberTeamDto> search(MemberSearchCondition condition){
		
		return queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"), 
						member.username,
						member.age,
						team.id.as("teamId"),
						team.name.as("teamName")))
				.from(member)
				.leftJoin(member.team, team)
				.where(userNameEq(condition.getUsername()),
					   teamNameEq(condition.getTeamName()),
					   ageGoe(condition.getAgeGoe()),
					   ageLoe(condition.getAgeLoe())
				)
				.fetch();
	}

	private BooleanExpression teamNameEq(String teamName) {
		// TODO Auto-generated method stub
		return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
	}
	
	private BooleanExpression userNameEq(String username) {
		return StringUtils.hasText(username) ? member.username.eq(username) : null;
	}
	private BooleanExpression ageLoe(Integer ageLoe) {
		// TODO Auto-generated method stub
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}

	private BooleanExpression ageGoe(Integer ageGoe) {
		// TODO Auto-generated method stub
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

	@Override
	public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
		 QueryResults<MemberTeamDto> results = queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"), 
						member.username,
						member.age,
						team.id.as("teamId"),
						team.name.as("teamName")))
				.from(member)
				.leftJoin(member.team, team)
				.where(userNameEq(condition.getUsername()),
					   teamNameEq(condition.getTeamName()),
					   ageGoe(condition.getAgeGoe()),
					   ageLoe(condition.getAgeLoe())
				)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetchResults(); //count쿼리랑 2번나감
		
		 List<MemberTeamDto> content = results.getResults();
		 long total = results.getTotal();
		 
		 return new PageImpl<>(content, pageable, total);
		
	}

	@Override
	public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
		List<MemberTeamDto> content = queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"), 
						member.username,
						member.age,
						team.id.as("teamId"),
						team.name.as("teamName")))
				.from(member)
				.leftJoin(member.team, team)
				.where(userNameEq(condition.getUsername()),
					   teamNameEq(condition.getTeamName()),
					   ageGoe(condition.getAgeGoe()),
					   ageLoe(condition.getAgeLoe())
				)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch(); 
		
		// 최적화를 위해 따로 쿼리 실행 가능.
/*		long total = queryFactory
						.select(new QMemberTeamDto(
								member.id.as("memberId"), 
								member.username,
								member.age,
								team.id.as("teamId"),
								team.name.as("teamName")))
						.from(member)
						.leftJoin(member.team, team)
						.where(userNameEq(condition.getUsername()),
							   teamNameEq(condition.getTeamName()),
							   ageGoe(condition.getAgeGoe()),
							   ageLoe(condition.getAgeLoe())
						)
						.fetchCount();*/
		 JPAQuery<MemberTeamDto> countQuery = queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"), 
						member.username,
						member.age,
						team.id.as("teamId"),
						team.name.as("teamName")))
				.from(member)
				.leftJoin(member.team, team)
				.where(userNameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe())
						);
		return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount()); 
//		 return new PageImpl<>(content, pageable, total);
	}

	
	

}
