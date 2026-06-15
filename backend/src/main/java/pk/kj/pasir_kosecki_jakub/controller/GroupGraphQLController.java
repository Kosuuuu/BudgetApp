package pk.kj.pasir_kosecki_jakub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.kj.pasir_kosecki_jakub.dto.GroupDTO;
import pk.kj.pasir_kosecki_jakub.model.Group;
import pk.kj.pasir_kosecki_jakub.service.GroupService;
import jakarta.validation.Valid;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class GroupGraphQLController {

    private final GroupService groupService;

    @QueryMapping
    public List<Group> groups() {
        return groupService.getAllGroups();
    }

    @QueryMapping
    public List<Group> myGroups() {
        return groupService.getMyGroups();
    }

    @MutationMapping
    public Group createGroup(@Argument @Valid GroupDTO groupDTO) {
        return groupService.createGroup(groupDTO);
    }

    @MutationMapping
    public Boolean deleteGroup(@Argument Long id) {
        return groupService.deleteGroup(id);
    }
}