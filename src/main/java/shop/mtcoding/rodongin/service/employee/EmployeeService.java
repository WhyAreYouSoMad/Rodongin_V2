package shop.mtcoding.rodongin.service.employee;

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import shop.mtcoding.rodongin.dto.employee.EmployeeCareerDto;
import shop.mtcoding.rodongin.dto.employee.EmployeeDetailOutDto;
import shop.mtcoding.rodongin.dto.employee.EmployeeGraduateDto;
import shop.mtcoding.rodongin.dto.employee.EmployeeJoinInDto;
import shop.mtcoding.rodongin.dto.employee.EmployeeLicenseDto;
import shop.mtcoding.rodongin.dto.employee.EmployeeLoginInDto;
import shop.mtcoding.rodongin.dto.employee.EmployeeStackDto;
import shop.mtcoding.rodongin.dto.employee.EmployeeUpdateInDto;
import shop.mtcoding.rodongin.dto.resume.ResumeDto;
import shop.mtcoding.rodongin.handler.ex.CustomApiException;
import shop.mtcoding.rodongin.handler.ex.CustomException;
import shop.mtcoding.rodongin.model.employee.Employee;
import shop.mtcoding.rodongin.model.employee.EmployeeCareerRepository;
import shop.mtcoding.rodongin.model.employee.EmployeeGraduateRepository;
import shop.mtcoding.rodongin.model.employee.EmployeeLicenseRepository;
import shop.mtcoding.rodongin.model.employee.EmployeeRepository;
import shop.mtcoding.rodongin.model.employee.EmployeeStackRepository;
import shop.mtcoding.rodongin.model.resume.ResumeRepository;
import shop.mtcoding.rodongin.util.Encode;
import shop.mtcoding.rodongin.util.PathUtil;

@RequiredArgsConstructor
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    private final EmployeeGraduateRepository employeeGraduateRepository;

    private final EmployeeCareerRepository employeeCareerRepository;

    private final EmployeeLicenseRepository employeeLicenseRepository;

    private final EmployeeStackRepository employeeStackRepository;

    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public EmployeeDetailOutDto 유저정보조회(int principalId) {
        Employee employee = employeeRepository.findById(principalId);
        List<EmployeeGraduateDto> graduates = employeeGraduateRepository.findByEmpId(principalId);
        List<EmployeeCareerDto> careers = employeeCareerRepository.findByEmpId(principalId);
        List<EmployeeLicenseDto> licenses = employeeLicenseRepository.findByEmpId(principalId);
        List<EmployeeStackDto> stacks = employeeStackRepository.findByEmpId(principalId);
        List<ResumeDto> resumes = resumeRepository.findByEmpId(principalId);

        EmployeeDetailOutDto employeeDetailInfo = EmployeeDetailOutDto.builder()
                .id(employee.getId())
                .employeeFullname(employee.getEmployeeFullname())
                .employeeName(employee.getEmployeeName())
                .employeeAddress(employee.getEmployeeAddress())
                .employeeBirth(employee.getEmployeeBirth())
                .employeeEmail(employee.getEmployeeEmail())
                .employeeTel(employee.getEmployeeTel())
                .employeeGraduateDtos(graduates)
                .employeeCareerDtos(careers)
                .employeeLicenseDtos(licenses)
                .employeeStackDtos(stacks)
                .resumeDtos(resumes)
                .build();

        return employeeDetailInfo;
    }

    @Transactional
    public void 회원가입(EmployeeJoinInDto employeeJoinInDto) {

        Employee sameEmployee = employeeRepository.findByEmployeeName(employeeJoinInDto.getEmployeeName());

        if (sameEmployee != null) {
            throw new CustomException("동일한 username이 존재합니다");
        }

        String encodedPassword = "";

        try {
            encodedPassword = Encode.passwordEncode(employeeJoinInDto.getEmployeePassword());

        } catch (Exception e) {
            throw new CustomException("비밀번호 해싱 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        employeeJoinInDto.setEmployeePassword(encodedPassword);
        System.out.println("테스트");

        String email = employeeJoinInDto.getEmployeeEmail().replaceAll(",", "");
        employeeJoinInDto.setEmployeeEmail(email);

        String tel = employeeJoinInDto.getEmployeeTel().replaceAll(",", "");
        employeeJoinInDto.setEmployeeTel(tel);

        String address = employeeJoinInDto.getEmployeeAddress().replaceAll(",", "");
        employeeJoinInDto.setEmployeeAddress(address);

        employeeRepository.insert(employeeJoinInDto);

        try {

        } catch (Exception e) {
            throw new CustomException("일시적인 서버 에러입니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Employee 로그인(EmployeeLoginInDto employeeLoginInDto, HttpServletResponse response, String employeeName) {

        Employee principalPS = employeeRepository.findByEmployeeName(employeeLoginInDto.getEmployeeName());
        if (principalPS == null) {
            throw new CustomException("일치하는 회원 정보가 없습니다.");
        }
        boolean isCheck;
        try {
            isCheck = Encode.matches(employeeLoginInDto.getEmployeePassword(), principalPS.getEmployeePassword());
        } catch (Exception e) {
            throw new CustomException("???");
        }

        if (!isCheck) {
            throw new CustomException("비밀번호가 다릅니다.");
        }
        employeeLoginInDto.setEmployeePassword(principalPS.getEmployeePassword());

        Employee principal = employeeRepository.findByEmployeeNameAndPassword(employeeLoginInDto);

        if (principal == null) {
            throw new CustomException("비밀번호가 일치하지 않습니다.");
        }

        if (employeeName == null || employeeName.isEmpty()) { // 쿠키 값
            employeeName = "";
        }

        if (employeeName.equals("on")) {
            Cookie cookie = new Cookie("remember", employeeLoginInDto.getEmployeeName());
            cookie.setMaxAge(60);
            cookie.setPath("/");
            response.addCookie(cookie);
        } else {
            Cookie cookie = new Cookie("remember", "");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }

        return principal;

    }

    @Transactional
    public Employee 회원정보수정(int principalId, EmployeeUpdateInDto employeeUpdateInDto) {
        System.out.println(employeeUpdateInDto);
        if (employeeUpdateInDto.getEmployeeThumbnail() != null) {
            String thumbnail = PathUtil.writeImageFile(employeeUpdateInDto.getEmployeeThumbnail());
            employeeUpdateInDto.setEmployeeThumbnail(thumbnail);
            System.out.println(thumbnail);
        }
        try {
            employeeRepository.updateById(principalId, employeeUpdateInDto);

        } catch (Exception e) {
            throw new CustomApiException("회원정보 수정에 실패하였습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        System.out.println("aaaaa");
        Employee principal = employeeRepository.findById(principalId);
        return principal;
    }
}
