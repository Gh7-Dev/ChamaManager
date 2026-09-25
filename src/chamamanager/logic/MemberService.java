/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.logic;

import chamamanager.dao.MemberDAO;
import chamamanager.model.Member;
import java.util.List;

/**
 * Handles member-related business operations.
 *
 * Coordinates member operations with {@link MemberDAO}.
 *
 * @author gh7
 */
public class MemberService {

    private final MemberDAO memberDAO;

    public MemberService(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    /**
     * Registers a new member.
     *
     * @param member the member to register
     */
    public void registerMember(Member member) {
        memberDAO.create(member);
    }

    /**
     * Retrieves a member by identifier.
     *
     * @param id the member identifier
     * @return the matching member, or {@code null}
     */
    public Member getMember(int id) {
        return memberDAO.getById(id);
    }

    /**
     * Lists all members.
     *
     * @return a list of all members
     */
    public List<Member> getAllMembers() {
        return memberDAO.getAll();
    }

    /**
     * Removes a member by identifier.
     *
     * @param id the member identifier
     */
    public void removeMember(int id) {
        memberDAO.delete(id);
    }
}
