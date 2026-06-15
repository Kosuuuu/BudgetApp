package pk.kj.pasir_kosecki_jakub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.kj.pasir_kosecki_jakub.dto.MembershipDTO;
import pk.kj.pasir_kosecki_jakub.model.Membership;
import pk.kj.pasir_kosecki_jakub.service.MembershipService;

import java.util.List;
import jakarta.validation.Valid;
@Controller
@RequiredArgsConstructor
public class MembershipGraphQLController {

    private final MembershipService membershipService;

    @QueryMapping
    public List<Membership> groupMembers(@Argument Long groupId) {
        return membershipService.getGroupMembers(groupId);
    }

    @MutationMapping
    public Membership addMember(@Argument @Valid MembershipDTO membershipDTO) {
        return membershipService.addMember(membershipDTO);
    }

    @MutationMapping
    public Boolean removeMember(@Argument Long membershipId) {
        return membershipService.removeMember(membershipId);
    }
}