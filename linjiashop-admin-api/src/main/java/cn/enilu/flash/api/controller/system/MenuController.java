package cn.enilu.flash.api.controller.system;

import cn.enilu.flash.bean.core.AuthorizationUser;
import cn.enilu.flash.bean.core.BussinessLog;
import cn.enilu.flash.bean.entity.system.Menu;
import cn.enilu.flash.bean.enumeration.Permission;
import cn.enilu.flash.bean.exception.ApplicationException;
import cn.enilu.flash.bean.exception.ApplicationExceptionEnum;
import cn.enilu.flash.bean.vo.front.Rets;
import cn.enilu.flash.bean.vo.node.*;
import cn.enilu.flash.cache.TokenCache;
import cn.enilu.flash.service.system.LogObjectHolder;
import cn.enilu.flash.service.system.MenuService;
import cn.enilu.flash.service.system.impl.ConstantFactory;
import cn.enilu.flash.utils.HttpUtil;
import cn.enilu.flash.utils.Maps;
import cn.enilu.flash.utils.StringUtil;
import cn.enilu.flash.web.controller.BaseController;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * MenuController
 *
 * @author enilu
 * @version 2018/9/12 0012
 */
@RestController
@RequestMapping("/menu")
public class MenuController extends BaseController {


    private Logger logger = LoggerFactory.getLogger(MenuController.class);
    @Autowired
    private MenuService menuService;
    @Autowired
    private TokenCache tokenCache;

    @RequestMapping(value = "/listForRouter", method = RequestMethod.GET)
    public Object listForRouter() {
        AuthorizationUser shiroUser = tokenCache.getUser(HttpUtil.getToken());

        List<RouterMenu> list = menuService.getSideBarMenus(shiroUser.getRoleList());
        return Rets.success(list);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Object list() {
        List<MenuNode> list = menuService.getMenus();
        return Rets.success(list);
    }

    @RequestMapping(value = "/tree", method = RequestMethod.GET)
    public Object tree() {
        List<MenuNode> list = menuService.getMenus();
        List<TreeSelectNode> treeSelectNodes = Lists.newArrayList();
        for (MenuNode menuNode : list) {
            TreeSelectNode tsn = transfer(menuNode);
            treeSelectNodes.add(tsn);
        }
        return Rets.success(treeSelectNodes);
    }

    public TreeSelectNode transfer(MenuNode node) {
        TreeSelectNode tsn = new TreeSelectNode();
        tsn.setId(node.getCode());
        tsn.setLabel(node.getName());
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            List<TreeSelectNode> children = Lists.newArrayList();
            for (MenuNode child : node.getChildren()) {
                children.add(transfer(child));
            }
            tsn.setChildren(children);
        }
        return tsn;
    }

    @RequestMapping(method = RequestMethod.POST)
    @BussinessLog(value = "????????????", key = "name")
    @RequiresPermissions(value = {Permission.MENU_EDIT})
    public Object save(@ModelAttribute @Valid Menu menu) {
        //???????????????????????????
        if (menu.getId() == null) {
            String existedMenuName = ConstantFactory.me().getMenuNameByCode(menu.getCode());
            if (StringUtil.isNotEmpty(existedMenuName)) {
                throw new ApplicationException(ApplicationExceptionEnum.EXISTED_THE_MENU);
            }
        }

        //????????????????????????
        menuService.menuSetPcode(menu);
        if (menu.getId() == null) {
            menuService.insert(menu);
        } else {
            menuService.update(menu);
        }
        return Rets.success();
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @BussinessLog(value = "????????????", key = "id")
    @RequiresPermissions(value = {Permission.MENU_DEL})
    public Object remove(@RequestParam Long id) {
        logger.info("id:{}", id);
        if (id == null) {
            throw new ApplicationException(ApplicationExceptionEnum.REQUEST_NULL);
        }
        //?????????????????????????????????????????????
        if (id.intValue() < 70) {
            return Rets.failure("???????????????????????????????????????");
        }
        //?????????????????????
        LogObjectHolder.me().set(ConstantFactory.me().getMenuName(id));
        menuService.delMenuContainSubMenus(id);
        return Rets.success();
    }

    /**
     * ???????????????
     */
    @RequestMapping(value = "/menuTreeListByRoleId", method = RequestMethod.GET)
    @RequiresPermissions(value = {Permission.MENU})
    public Object menuTreeListByRoleId(Integer roleId) {
        List<Long> menuIds = menuService.getMenuIdsByRoleId(roleId);
        List<ZTreeNode> roleTreeList = null;
        if (menuIds == null || menuIds.isEmpty()) {
            roleTreeList = menuService.menuTreeList(null);
        } else {
            roleTreeList = menuService.menuTreeList(menuIds);

        }
        List<Node> list = menuService.generateMenuTreeForRole(roleTreeList);

        //element-ui???tree?????????????????????????????????????????????????????????????????????????????????????????????????????????
        Map<Long, ZTreeNode> map = cn.enilu.flash.utils.Lists.toMap(roleTreeList, "id");
        Map<Long, List<ZTreeNode>> group = cn.enilu.flash.utils.Lists.group(roleTreeList, "pId");
        for (Map.Entry<Long, List<ZTreeNode>> entry : group.entrySet()) {
            if (entry.getValue().size() > 1) {
                roleTreeList.remove(map.get(entry.getKey()));
            }
        }

        List<Long> checkedIds = Lists.newArrayList();
        for (ZTreeNode zTreeNode : roleTreeList) {
            if (zTreeNode.getChecked() != null && zTreeNode.getChecked()
                    && zTreeNode.getpId().intValue() != 0) {
                checkedIds.add(zTreeNode.getId());
            }
        }
        return Rets.success(Maps.newHashMap("treeData", list, "checkedIds", checkedIds));
    }
}
