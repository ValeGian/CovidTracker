package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionNodeManager;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces.*;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = "RegionNodeManagerEJB")
public class RegionNodeManagerBean implements RegionNodeManager {

    @EJB RegionValleDAosta regionValleDAosta;
    @EJB RegionPiemonte regionPiemonte;
    @EJB RegionSicilia regionSicilia;
    /*@EJB RegionLiguria regionLiguria;
    @EJB RegionLombardia regionLombardia;
    @EJB RegionTrentinoAltoAdige regionTrentinoAltoAdige;
    @EJB RegionVeneto regionVeneto;
    @EJB RegionFriuliVeneziaGiulia regionFriuliVeneziaGiulia;
    @EJB RegionEmiliaRomagna regionEmiliaRomagna;
    @EJB RegionToscana regionToscana;
    @EJB RegionUmbria regionUmbria;
    @EJB RegionMarche regionMarche;
    @EJB RegionLazio regionLazio;
    @EJB RegionAbruzzo regionAbruzzo;
    @EJB RegionMolise regionMolise;
    @EJB RegionCampania regionCampania;
    @EJB RegionPuglia regionPuglia;
    @EJB RegionBasilicata regionBasilicata;
    @EJB RegionCalabria regionCalabria;
    @EJB RegionSardegna regionSardegna;
    */

    public RegionNodeManagerBean() {
    }

    @Override
    public RegionNode getRegion(String ejb) {
        switch(ejb)
        {
            case "valledaosta":
                return regionValleDAosta;
            case "piemonte":
                return regionPiemonte;
            case "sicilia":
                return regionSicilia;
            /*case "liguria":
                return regionLiguria;
            case "lombardia":
                return regionLombardia;
            case "trentinoaltoadige":
                return regionTrentinoAltoAdige;
            case "veneto":
                return regionVeneto;
            case "friuliveneziagiulia":
                return regionFriuliVeneziaGiulia;
            case "emilliaromagna":
                return regionEmiliaRomagna;
            case "toscana":
                return regionToscana;
            case "umbria":
                return regionUmbria;
            case "marche":
                return regionMarche;
            case "lazio":
                return regionLazio;
            case "abruzzo":
                return regionAbruzzo;
            case "molise":
                return regionMolise;
            case "campania":
                return regionCampania;
            case "puglia":
                return regionPuglia;
            case "basilicata":
                return regionBasilicata;
            case "calabria":
                return regionCalabria;
            case "sardegna":
                return regionSardegna;
             */
        }
        return null;
    }
}
