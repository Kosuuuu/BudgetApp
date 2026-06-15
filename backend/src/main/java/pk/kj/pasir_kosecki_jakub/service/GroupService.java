package pk.kj.pasir_kosecki_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pk.kj.pasir_kosecki_jakub.dto.GroupDTO;
import pk.kj.pasir_kosecki_jakub.model.Group;
import pk.kj.pasir_kosecki_jakub.model.Membership;
import pk.kj.pasir_kosecki_jakub.model.User;
import pk.kj.pasir_kosecki_jakub.repository.DebtRepository;
import pk.kj.pasir_kosecki_jakub.repository.GroupRepository;
import pk.kj.pasir_kosecki_jakub.repository.MembershipRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public List<Group> getMyGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser);
    }

    @Transactional
    public Group createGroup(GroupDTO groupDTO) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(currentUser);

        Group savedGroup = groupRepository.save(group);

        Membership membership = new Membership();
        membership.setGroup(savedGroup);
        membership.setUser(currentUser);

        membershipRepository.save(membership);

        return savedGroup;
    }

    @Transactional
    public boolean deleteGroup(Long id) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy o ID: " + id));

        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Tylko właściciel może usunąć grupę");
        }

        debtRepository.deleteByGroup_Id(id);
        membershipRepository.deleteByGroup_Id(id);
        groupRepository.delete(group);

        return true;
    }
}