package pk.kj.pasir_kosecki_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pk.kj.pasir_kosecki_jakub.dto.MembershipDTO;
import pk.kj.pasir_kosecki_jakub.model.Group;
import pk.kj.pasir_kosecki_jakub.model.Membership;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.GroupRepository;
import pk.kj.pasir_kosecki_jakub.repository.MembershipRepository;
import pk.kj.pasir_kosecki_jakub.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public List<Membership> getGroupMembers(Long groupId) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + groupId
                ));

        boolean isOwner = group.getOwner().getId().equals(currentUser.getId());
        boolean isMember = membershipRepository.existsByGroup_IdAndUser_Id(groupId, currentUser.getId());

        if (!isOwner && !isMember) {
            throw new RuntimeException("Nie masz dostępu do tej grupy");
        }

        return membershipRepository.findByGroup_Id(groupId);
    }

    public Membership addMember(MembershipDTO membershipDTO) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = groupRepository.findById(membershipDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + membershipDTO.getGroupId()
                ));

        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Tylko właściciel grupy może dodawać członków");
        }

        User userToAdd = userRepository.findByEmail(membershipDTO.getUserEmail())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono użytkownika o emailu: " + membershipDTO.getUserEmail()
                ));

        boolean alreadyExists = membershipRepository.existsByGroup_IdAndUser_Id(
                group.getId(),
                userToAdd.getId()
        );

        if (alreadyExists) {
            throw new RuntimeException("Ten użytkownik już należy do grupy");
        }

        Membership membership = new Membership();
        membership.setGroup(group);
        membership.setUser(userToAdd);

        return membershipRepository.save(membership);
    }

    public boolean removeMember(Long membershipId) {
        User currentUser = currentUserService.getCurrentUser();

        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono członkostwa o ID: " + membershipId
                ));

        Group group = membership.getGroup();

        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Tylko właściciel grupy może usuwać członków");
        }

        if (group.getOwner().getId().equals(membership.getUser().getId())) {
            throw new RuntimeException("Nie można usunąć właściciela grupy z członków");
        }

        membershipRepository.delete(membership);

        return true;
    }

    public boolean isCurrentUserMemberOfGroup(Long groupId) {
        User currentUser = currentUserService.getCurrentUser();
        return membershipRepository.existsByGroup_IdAndUser_Id(groupId, currentUser.getId());
    }

    public boolean isCurrentUserOwnerOrMember(Long groupId) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + groupId
                ));

        boolean isOwner = group.getOwner().getId().equals(currentUser.getId());
        boolean isMember = membershipRepository.existsByGroup_IdAndUser_Id(groupId, currentUser.getId());

        return isOwner || isMember;
    }
}