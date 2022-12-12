package toy.ktx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import toy.ktx.domain.Deploy;
import toy.ktx.domain.Train;
import toy.ktx.domain.dto.DeployForm;
import toy.ktx.domain.dto.PassengerDto;
import toy.ktx.domain.dto.projections.NormalSeatDto;
import toy.ktx.domain.enums.Grade;
import toy.ktx.domain.ktx.*;
import toy.ktx.service.DeployService;
import toy.ktx.service.KtxRoomService;
import toy.ktx.service.KtxSeatService;
import toy.ktx.service.KtxService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SeatController {

    private final KtxRoomService ktxRoomService;
    private final KtxSeatService ktxSeatService;
    private final KtxService ktxService;
    private final DeployService deployService;

    private final String[] alpha = {"A", "B", "C", "D"};
    Map seat = new HashMap();

    @PostMapping("/seat")
    public String chooseSeat(@ModelAttribute DeployForm deployForm,
                             BindingResult bindingResult,
                             @RequestParam(required = false) String prevGoing,
                             @RequestParam(required = false) String nextGoing,
                             @RequestParam(required = false) String prevComing,
                             @RequestParam(required = false) String nextComing,
                             @RequestParam(required = false) String dateTimeOfGoing,
                             @RequestParam(required = false) String dateTimeOfLeaving,
                             @RequestParam(required = false) String departurePlace,
                             @RequestParam(required = false) String arrivalPlace,
                             @RequestParam(required = false) Boolean round,
                             @ModelAttribute PassengerDto passengerDto,
                             Model model) {

        model.addAttribute("departurePlace", departurePlace);
        model.addAttribute("arrivalPlace", arrivalPlace);
        model.addAttribute("round", round);
        model.addAttribute("passengers", passengerDto.howManyOccupied());

        if (round == true) {
            LocalDateTime beforeDateTime = getLocalDateTime(dateTimeOfGoing);
            LocalDateTime afterDateTime = getLocalDateTime(dateTimeOfLeaving);

            List<Deploy> deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, beforeDateTime);
            //오는 날에는 가는 날의 출발지가 도착지고 도착지가 출발지임 따라서 getArrivalPlace가 departurePlace(출발지)에 위치해야 됨
            List<Deploy> deploysWhenComing = deployService.searchDeploy(arrivalPlace, departurePlace, afterDateTime);

            LocalDateTime dateTime = null;
            Boolean noBefore = false;
            Boolean noAfter = false;

            if (prevGoing != null) {
                LocalDateTime newTime = beforeDateTime.minusDays(1);

                if (newTime.isBefore(LocalDateTime.now()) && newTime.getDayOfMonth() != LocalDateTime.now().getDayOfMonth()) {
                    bindingResult.reject("noBefore", null);

                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", afterDateTime);

                    dateTime = beforeDateTime;
                    noBefore = true;
                } else {
                    model.addAttribute("before", newTime);
                    model.addAttribute("after", afterDateTime);

                    int year = newTime.getYear();
                    int monthValue = newTime.getMonthValue();
                    int dayOfMonth = newTime.getDayOfMonth();

                    dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                    deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, dateTime);
                }

                if (newTime.isBefore(LocalDateTime.now()) && newTime.getDayOfMonth() == LocalDateTime.now().getDayOfMonth()) {
                    model.addAttribute("before", newTime);
                    model.addAttribute("after", afterDateTime);

                    dateTime = LocalDateTime.now();

                    deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, dateTime);
                }

                if (deploysWhenGoing.isEmpty() == true && deploysWhenComing.isEmpty() == true) {
                    model.addAttribute("emptyWhenGoing", true);
                    model.addAttribute("emptyWhenComing", true);

                    model.addAttribute("dateTimeOfGoing", dateTime.toString());
                    model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                    return "schedule";
                }

                if (deploysWhenGoing.isEmpty() == true) {
                    model.addAttribute("emptyWhenGoing", true);
                    model.addAttribute("deploysWhenComing", deploysWhenComing);

                    model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));
                    deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTime.toString());
                    model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                    return "schedule";
                }

                if (deploysWhenGoing.isEmpty() == false && deploysWhenComing.isEmpty() == true) {
                    model.addAttribute("emptyWhenComing", true);
                    model.addAttribute("deploysWhenGoing", deploysWhenGoing);

                    model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                    deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTime.toString());
                    model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                    return "schedule";
                }

                model.addAttribute("deploysWhenGoing", deploysWhenGoing);
                model.addAttribute("deploysWhenComing", deploysWhenComing);

                model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));

                model.addAttribute("dateTimeOfGoing", dateTime.toString());
                model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);

                deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());
                deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());
                return "schedule";
            }

            if (nextGoing != null) {
                LocalDateTime newTime = beforeDateTime.plusDays(1);

                if (newTime.isAfter(LocalDateTime.now().plusDays(30)) && newTime.getDayOfMonth() != LocalDateTime.now().plusDays(30).getDayOfMonth()) {
                    bindingResult.reject("noAfter", null);

                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", afterDateTime);

                    dateTime = beforeDateTime;
                    noAfter = true;
                } else {
                    model.addAttribute("before", newTime);
                    model.addAttribute("after", afterDateTime);

                    int year = newTime.getYear();
                    int monthValue = newTime.getMonthValue();
                    int dayOfMonth = newTime.getDayOfMonth();

                    dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                    deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, dateTime);
                }

                if (newTime.isAfter(LocalDateTime.now().plusDays(30)) && newTime.getDayOfMonth() == LocalDateTime.now().plusDays(30).getDayOfMonth()) {
                    model.addAttribute("before", newTime);
                    model.addAttribute("after", afterDateTime);

                    int year = LocalDateTime.now().plusDays(30).getYear();
                    int monthValue = LocalDateTime.now().plusDays(30).getMonthValue();
                    int dayOfMonth = LocalDateTime.now().plusDays(30).getDayOfMonth();

                    dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                    deploysWhenGoing = deployService.searchDeploy(arrivalPlace, departurePlace, dateTime);
                }

                if (deploysWhenGoing.isEmpty() == true && deploysWhenComing.isEmpty() == true) {
                    model.addAttribute("emptyWhenGoing", true);
                    model.addAttribute("emptyWhenComing", true);

                    model.addAttribute("dateTimeOfGoing", dateTime.toString());
                    model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                    return "schedule";
                }

                if (deploysWhenGoing.isEmpty() == true) {
                    model.addAttribute("emptyWhenGoing", true);
                    model.addAttribute("deploysWhenComing", deploysWhenComing);

                    model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));
                    deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTime.toString());
                    model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                    return "schedule";
                }

                if (deploysWhenGoing.isEmpty() == false && deploysWhenComing.isEmpty() == true) {
                    model.addAttribute("emptyWhenComing", true);
                    model.addAttribute("deploysWhenGoing", deploysWhenGoing);

                    model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                    deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTime.toString());
                    model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                    return "schedule";
                }

                model.addAttribute("deploysWhenGoing", deploysWhenGoing);
                model.addAttribute("deploysWhenComing", deploysWhenComing);

                model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));

                model.addAttribute("dateTimeOfGoing", dateTime.toString());
                model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);

                deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());
                deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());
                return "schedule";
            }

            if (prevComing != null) {
                LocalDateTime newTime = afterDateTime.minusDays(1);

                if (newTime.isBefore(LocalDateTime.now()) && newTime.getDayOfMonth() != LocalDateTime.now().getDayOfMonth()) {
                    bindingResult.reject("noBefore", null);

                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", afterDateTime);

                    dateTime = afterDateTime;
                    noBefore = true;
                } else {
                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", newTime);

                    int year = newTime.getYear();
                    int monthValue = newTime.getMonthValue();
                    int dayOfMonth = newTime.getDayOfMonth();

                    dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                    deploysWhenComing = deployService.searchDeploy(arrivalPlace, departurePlace, dateTime);
                }

                if (newTime.isBefore(LocalDateTime.now()) && newTime.getDayOfMonth() == LocalDateTime.now().getDayOfMonth()) {
                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", newTime);

                    dateTime = LocalDateTime.now();

                    deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, dateTime);
                }

                if (deploysWhenComing.isEmpty() == true && deploysWhenGoing.isEmpty() == true) {
                    model.addAttribute("emptyWhenComing", true);
                    model.addAttribute("emptyWhenGoing", true);

                    model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                    model.addAttribute("dateTimeOfLeaving", dateTime.toString());
                    return "schedule";
                }

                if (deploysWhenComing.isEmpty() == true) {
                    model.addAttribute("emptyWhenComing", true);
                    model.addAttribute("deploysWhenGoing", deploysWhenGoing);

                    model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                    deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                    model.addAttribute("dateTimeOfLeaving", dateTime.toString());
                    return "schedule";
                }

                if (deploysWhenComing.isEmpty() == false && deploysWhenGoing.isEmpty() == true) {
                    model.addAttribute("emptyWhenGoing", true);
                    model.addAttribute("deploysWhenComing", deploysWhenComing);

                    model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));
                    deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                    model.addAttribute("dateTimeOfLeaving", dateTime.toString());
                    return "schedule";
                }

                model.addAttribute("deploysWhenGoing", deploysWhenGoing);
                model.addAttribute("deploysWhenComing", deploysWhenComing);

                model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));

                model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                model.addAttribute("dateTimeOfLeaving", dateTime.toString());

                deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());
                deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());
                return "schedule";
            }

            if (nextComing != null) {
                LocalDateTime newTime = afterDateTime.plusDays(1);

                if (newTime.isAfter(LocalDateTime.now().plusDays(30)) && newTime.getDayOfMonth() != LocalDateTime.now().plusDays(30).getDayOfMonth()) {
                    bindingResult.reject("noAfter", null);

                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", afterDateTime);

                    dateTime = afterDateTime;
                    noAfter = true;
                } else {
                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", newTime);

                    int year = newTime.getYear();
                    int monthValue = newTime.getMonthValue();
                    int dayOfMonth = newTime.getDayOfMonth();

                    dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                    deploysWhenComing = deployService.searchDeploy(arrivalPlace, departurePlace, dateTime);
                }

                if (newTime.isAfter(LocalDateTime.now().plusDays(30)) && newTime.getDayOfMonth() == LocalDateTime.now().plusDays(30).getDayOfMonth()) {
                    model.addAttribute("before", beforeDateTime);
                    model.addAttribute("after", newTime);

                    int year = LocalDateTime.now().plusDays(30).getYear();
                    int monthValue = LocalDateTime.now().plusDays(30).getMonthValue();
                    int dayOfMonth = LocalDateTime.now().plusDays(30).getDayOfMonth();

                    dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                    deploysWhenComing = deployService.searchDeploy(arrivalPlace, departurePlace, dateTime);
                }

                if (deploysWhenComing.isEmpty() == true && deploysWhenGoing.isEmpty() == true) {
                    model.addAttribute("emptyWhenComing", true);
                    model.addAttribute("emptyWhenGoing", true);

                    model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                    model.addAttribute("dateTimeOfLeaving", dateTime.toString());
                    return "schedule";
                }

                if (deploysWhenComing.isEmpty() == true) {
                    model.addAttribute("emptyWhenComing", true);
                    model.addAttribute("deploysWhenGoing", deploysWhenGoing);

                    model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                    deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                    model.addAttribute("dateTimeOfLeaving", dateTime.toString());
                    return "schedule";
                }

                if (deploysWhenComing.isEmpty() == false && deploysWhenGoing.isEmpty() == true) {
                    model.addAttribute("emptyWhenGoing", true);
                    model.addAttribute("deploysWhenComing", deploysWhenComing);

                    model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));
                    deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());

                    model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                    model.addAttribute("dateTimeOfLeaving", dateTime.toString());
                    return "schedule";
                }

                model.addAttribute("deploysWhenGoing", deploysWhenGoing);
                model.addAttribute("deploysWhenComing", deploysWhenComing);

                model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));
                model.addAttribute("durationsWhenComing", getDuration(deploysWhenComing));

                model.addAttribute("dateTimeOfGoing", dateTimeOfGoing);
                model.addAttribute("dateTimeOfLeaving", dateTime.toString());

                deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());
                deployForm.setDeployIdOfComing(deploysWhenComing.get(0).getId());
                return "schedule";
            }

            //success logic
//            List<KtxSeat> ktxSeats = ktxSeatService.findKtxSeatWithKtxRoomWithTrainWithDeploy(deployForm.getDeployIdOfGoing());
            Deploy deploy = deployService.getDeployWithTrain(deployForm.getDeployIdOfGoing());
            Ktx train = (Ktx) deploy.getTrain();
            List<KtxRoom> ktxRooms = ktxRoomService.getKtxRoomWithSeatFetch(train.getId());

            List<String> normalReserveOkList = new ArrayList<>();
            List<String> vipReserveOkList = new ArrayList<>();

            for (KtxRoom ktxRoom : ktxRooms) {
                if (ktxRoom.getGrade() == Grade.NORMAL) {
                    KtxSeatNormal ktxSeatNormal = (KtxSeatNormal) ktxRoom.getKtxSeat();
                    if (ktxSeatNormal.howManyRemain(passengerDto.howManyOccupied()) != null) {
                        normalReserveOkList.add(ktxRoom.getRoomName());
                    }
                }
                else {
                    KtxSeatVip ktxSeatVip = (KtxSeatVip) ktxRoom.getKtxSeat();
                    if (ktxSeatVip.howManyRemain(passengerDto.howManyOccupied()) != null) {
                        vipReserveOkList.add(ktxRoom.getRoomName());
                    }
                }
            }

//            for (KtxSeat ktxSeat : ktxSeats) {
//                if (ktxSeat.getKtxRoom().getGrade() == Grade.NORMAL) {
//                    KtxSeatNormal ktxSeatNormal = (KtxSeatNormal) ktxSeat;
//                    if (ktxSeatNormal.howManyRemain(passengerDto.howManyOccupied()) != null) {
//                        normalReserveOkList.add(ktxSeat.getKtxRoom().getRoomName());
//                    }
//                }
//                else {
//                    KtxSeatVip ktxSeatVip = (KtxSeatVip) ktxSeat;
//                    if (ktxSeatVip.howManyRemain(passengerDto.howManyOccupied()) != null) {
//                        vipReserveOkList.add(ktxSeat.getKtxRoom().getRoomName());
//                    }
//                }
//            }

            log.info("시발 ={}", normalReserveOkList);
            log.info("시발 ={}", vipReserveOkList);

            //updated
            if(normalReserveOkList.isEmpty()) {
                model.addAttribute("normalDisabled", true);
                model.addAttribute("normalReserveOkList", normalReserveOkList);
            }

            if(vipReserveOkList.isEmpty()) {
                model.addAttribute("vipDisabled", true);
                model.addAttribute("vipReserveOkList", vipReserveOkList);
            }

            model.addAttribute("round", true);
            model.addAttribute("going", true);
            model.addAttribute("dateTimeOfGoing", beforeDateTime);
            model.addAttribute("dateTimeOfLeaving", afterDateTime);

            return "normalVip";
        }
// --------------------------------------------------------------------------------------------------------------------------
        LocalDateTime beforeDateTime = getLocalDateTime(dateTimeOfGoing);
        List<Deploy> deploysWhenGoing = deployService.searchDeployWithTrain(departurePlace, arrivalPlace, beforeDateTime);

        LocalDateTime dateTime = null;
        Boolean noBefore = false;
        Boolean noAfter = false;

        if (prevGoing != null) {
            LocalDateTime newTime = beforeDateTime.minusDays(1);

            if (newTime.isBefore(LocalDateTime.now()) && newTime.getDayOfMonth() != LocalDateTime.now().getDayOfMonth()) {
                bindingResult.reject("noBefore", null);

                model.addAttribute("before", beforeDateTime);

                dateTime = beforeDateTime;
                noBefore = true;
            } else {
                model.addAttribute("before", newTime);

                int year = newTime.getYear();
                int monthValue = newTime.getMonthValue();
                int dayOfMonth = newTime.getDayOfMonth();

                dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, dateTime);
            }

            if (newTime.isBefore(LocalDateTime.now()) && newTime.getDayOfMonth() == LocalDateTime.now().getDayOfMonth()) {
                model.addAttribute("before", newTime);

                dateTime = LocalDateTime.now();

                deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, dateTime);
            }

            if (deploysWhenGoing.isEmpty() == true) {
                model.addAttribute("emptyWhenGoing", true);

                model.addAttribute("dateTimeOfGoing", dateTime.toString());
                model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                return "schedule";
            }

            model.addAttribute("deploysWhenGoing", deploysWhenGoing);
            model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));

            model.addAttribute("dateTimeOfGoing", dateTime.toString());
            model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);

            deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());
            return "schedule";
        }

        if (nextGoing != null) {
            LocalDateTime newTime = beforeDateTime.plusDays(1);

            if (newTime.isAfter(LocalDateTime.now().plusDays(30)) && newTime.getDayOfMonth() != LocalDateTime.now().plusDays(30).getDayOfMonth()) {
                bindingResult.reject("noAfter", null);

                model.addAttribute("before", beforeDateTime);

                dateTime = beforeDateTime;
                noAfter = true;
            } else {
                model.addAttribute("before", newTime);

                int year = newTime.getYear();
                int monthValue = newTime.getMonthValue();
                int dayOfMonth = newTime.getDayOfMonth();

                dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                deploysWhenGoing = deployService.searchDeploy(departurePlace, arrivalPlace, dateTime);
            }

            if (newTime.isAfter(LocalDateTime.now().plusDays(30)) && newTime.getDayOfMonth() == LocalDateTime.now().plusDays(30).getDayOfMonth()) {
                model.addAttribute("before", newTime);

                int year = LocalDateTime.now().plusDays(30).getYear();
                int monthValue = LocalDateTime.now().plusDays(30).getMonthValue();
                int dayOfMonth = LocalDateTime.now().plusDays(30).getDayOfMonth();

                dateTime = LocalDateTime.of(year, monthValue, dayOfMonth, 0, 0);
                deploysWhenGoing = deployService.searchDeploy(arrivalPlace, departurePlace, dateTime);
            }

            if (deploysWhenGoing.isEmpty() == true) {
                model.addAttribute("emptyWhenGoing", true);

                model.addAttribute("dateTimeOfGoing", dateTime.toString());
                model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);
                return "schedule";
            }

            model.addAttribute("deploysWhenGoing", deploysWhenGoing);
            model.addAttribute("durationsWhenGoing", getDuration(deploysWhenGoing));

            model.addAttribute("dateTimeOfGoing", dateTime.toString());
            model.addAttribute("dateTimeOfLeaving", dateTimeOfLeaving);

            deployForm.setDeployIdOfGoing(deploysWhenGoing.get(0).getId());
            return "schedule";
        }

        //success Logic
//        List<KtxSeat> ktxSeats = ktxSeatService.findKtxSeatWithKtxRoomWithTrainWithDeploy(deployForm.getDeployIdOfGoing());
        Deploy deploy = deployService.getDeployWithTrain(deployForm.getDeployIdOfGoing());
        log.info("fuck = {}", deploy.getTrain().getClass());
        Ktx train = (Ktx) deploy.getTrain();
        List<KtxRoom> ktxRooms = ktxRoomService.getKtxRoomWithSeatFetch(train.getId());

        List<String> normalReserveOkList = new ArrayList<>();
        List<String> vipReserveOkList = new ArrayList<>();

        for (KtxRoom ktxRoom : ktxRooms) {
            if (ktxRoom.getGrade() == Grade.NORMAL) {
                KtxSeatNormal ktxSeatNormal = (KtxSeatNormal) ktxRoom.getKtxSeat();
                if (ktxSeatNormal.howManyRemain(passengerDto.howManyOccupied()) != null) {
                    normalReserveOkList.add(ktxRoom.getRoomName());
                }
            }
            else {
                KtxSeatVip ktxSeatVip = (KtxSeatVip) ktxRoom.getKtxSeat();
                if (ktxSeatVip.howManyRemain(passengerDto.howManyOccupied()) != null) {
                    vipReserveOkList.add(ktxRoom.getRoomName());
                }
            }
        }

//        for (KtxSeat ktxSeat : ktxSeats) {
//            if (ktxSeat.getKtxRoom().getGrade() == Grade.NORMAL) {
//                KtxSeatNormal ktxSeatNormal = (KtxSeatNormal) ktxSeat;
//                if (ktxSeatNormal.howManyRemain(passengerDto.howManyOccupied()) != null) {
//                    normalReserveOkList.add(ktxSeat.getKtxRoom().getRoomName());
//                }
//            }
//            else {
//                KtxSeatVip ktxSeatVip = (KtxSeatVip) ktxSeat;
//                if (ktxSeatVip.howManyRemain(passengerDto.howManyOccupied()) != null) {
//                    vipReserveOkList.add(ktxSeat.getKtxRoom().getRoomName());
//                }
//            }
//        }

        log.info("시발 ={}", normalReserveOkList);
        log.info("시발 ={}", vipReserveOkList);

        if(normalReserveOkList.isEmpty()) {
            model.addAttribute("normalDisabled", true);
        }

        if(vipReserveOkList.isEmpty()) {
            model.addAttribute("vipDisabled", true);
        }

        model.addAttribute("going", true);
        model.addAttribute("dateTimeOfGoing", beforeDateTime);

        return "normalVip";
    }

    private List<String> getDuration(List<Deploy> deploys) {
        List<String> durations = new ArrayList<>();
        for (Deploy deploy : deploys) {
            LocalDateTime departureTime = deploy.getDepartureTime();
            LocalDateTime arrivalTime = deploy.getArrivalTime();

            Duration duration = Duration.between(departureTime, arrivalTime);

            long toMinute = duration.getSeconds() / Long.valueOf(60);
            long hour = toMinute / Long.valueOf(60);
            long minute = toMinute % Long.valueOf(60);

            durations.add(hour + "시간 " + minute + "분");
        }
        return durations;
    }

    private LocalDateTime getLocalDateTime(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return LocalDateTime.parse(dateTime, formatter);
    }
}
