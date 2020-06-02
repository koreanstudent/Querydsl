package study.querydsl.repository;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import study.querydsl.entity.Member;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

	@Autowired
	EntityManager em;
	
	@Autowired
	MemberJpaRepository memberJpaRepository;
	
	@Test
	public void basicTest() {
		
		Member member = new Member("member1", 10);
		memberJpaRepository.save(member);
		
		Member findMember = memberJpaRepository.findById(member.getId()).get();
		
		List<Member> result1 = memberJpaRepository.findAll();
		List<Member> result2 = memberJpaRepository.findAll_Querydsl();
		
		List<Member> result3 = memberJpaRepository.findByUsername("member1");
		List<Member> result4 = memberJpaRepository.findByUsername_Querydsl("member1");
		
	}
	


	

}
