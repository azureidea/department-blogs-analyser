package com.github.dba.controller;

import com.github.dba.html.CsdnFetcher;
import com.github.dba.html.IteyeFetcher;
import com.github.dba.model.Author;
import com.github.dba.model.Blog;
import com.github.dba.model.DepGroup;
import com.github.dba.model.DepMember;
import com.github.dba.repo.read.BlogReadRepository;
import com.github.dba.repo.write.DepGroupWriteRepository;
import com.github.dba.repo.write.DepMemberWriteRepository;
import com.google.common.base.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

@Controller
public class MainController {
    private static final Log log = LogFactory.getLog(MainController.class);

    @Autowired
    private CsdnFetcher csdnFetcher;

    @Autowired
    private IteyeFetcher iteyeFetcher;

    @Autowired
    private DepGroupWriteRepository depGroupWriteRepository;

    @Autowired
    private DepMemberWriteRepository depMemberWriteRepository;

    @Autowired
    private BlogReadRepository blogReadRepository;

    @Value("${urls}")
    private String urls;

    @Value("${groups}")
    private String groups;

    @Value("${members}")
    private String members;
    private final ObjectMapper mapper = new ObjectMapper();

    @RequestMapping(value = "/blog/fetch", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public void blogFetch() throws Exception {
        log.debug("blog fetch start");
        String[] urlArray = urls.split(",");
        log.debug("urls:" + Arrays.toString(urlArray));

        for (String url : urlArray) {
            if (url.contains(CsdnFetcher.CSDN_KEY_WORD)) {
                csdnFetcher.fetch(url);
                continue;
            }
            iteyeFetcher.fetch(url);
        }
        log.debug("blog fetch finish");
    }

    @RequestMapping(value = "/group/create", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public void createGroups() {
        log.debug("create groups start");
        log.debug("group names:" + groups);
        depGroupWriteRepository.deleteAll();

        String[] groupNames = groups.split(",");
        for (String groupName : groupNames) {
            String[] texts = groupName.split("-");
            depGroupWriteRepository.save(new DepGroup(texts[0], texts[1]));
        }
        log.debug("create groups finish");
    }

    @RequestMapping(value = "/member/create", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public void createMembers() {
        log.debug("create members start");
        log.debug("member names:" + members);
        depMemberWriteRepository.deleteAll();

        String[] membersArray = members.split(",");
        for (String member : membersArray) {
            String[] texts = member.split("-");
            depMemberWriteRepository.save(new DepMember(texts[0], texts[1], texts[2]));
        }
        log.debug("create members finish");
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    public
    @ResponseBody
    String search(@RequestParam("depGroup") final String depGroup,
                  @RequestParam("website") final String website) throws Exception {
        log.debug("search blog start");
        log.debug(format("group name:%s", depGroup));
        log.debug(format("website:%s", website));

        Specification<Blog> spec = Specifications.where(new Specification<Blog>() {
            @Override
            public Predicate toPredicate(Root<Blog> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                Predicate predicate = cb.conjunction();

                if (!Strings.isNullOrEmpty(depGroup) && !"所有分组".equals(depGroup)) {
                    predicate.getExpressions().add(
                            cb.equal(root.<Author>get("author").<String>get("groupName"), depGroup));
                }

                if (!Strings.isNullOrEmpty(website) && !"所有".equals(website)) {
                    predicate.getExpressions().add(
                            cb.equal(root.<String>get("website"), website));
                }

                return predicate;
            }
        });

        List<Blog> blogs = blogReadRepository.findAll(spec);
        String resultArrayJson = mapper.writeValueAsString(blogs);
        log.debug(format("resultArrayJson: %s", resultArrayJson));
        log.debug("search blog finish");
        return resultArrayJson;
    }
}
